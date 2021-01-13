/*
 * MIT License
 *
 * Copyright (c) 2021 Justin Heflin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.heretere.hdl;

import com.heretere.hdl.dependency.DependencyLoader;
import com.heretere.hdl.dependency.annotation.LoaderPriority;
import com.heretere.hdl.dependency.builder.DependencyProvider;
import com.heretere.hdl.dependency.maven.MavenDependencyLoader;
import com.heretere.hdl.exception.DependencyLoadException;
import com.heretere.hdl.relocation.RelocatableDependencyLoader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * This class is responsible for caching {@link DependencyLoader} instances a running them in a sorted order.
 */
public class DependencyEngine {
    /**
     * The base path for dependencies.
     */
    private final @NotNull Path basePath;
    /**
     * A map containing all the dependency loaders for this dependency engine.
     */
    private final @NotNull Map<@NotNull Class<@NotNull ?>, @NotNull DependencyLoader<@NotNull ?>> dependencyLoaders;

    /**
     * Creates a new dependency engine with the specified base path.
     *
     * @param basePath The base path for this dependency engine.
     */
    protected DependencyEngine(final @NotNull Path basePath) {
        this.basePath = basePath;
        this.dependencyLoaders = new IdentityHashMap<>();
    }

    /**
     * Creates a new Dependency Engine with the specified base path.
     * It includes all the default DependencyLoader's by default.
     *
     * @param basePath The base path for all dependencies to be downloaded.
     * @return A new {@link DependencyEngine}.
     */
    @Contract("_ -> new")
    public static @NotNull DependencyEngine createNew(final @NotNull Path basePath) {
        return DependencyEngine.createNew(basePath, true);
    }

    /**
     * Creates a new Dependency Engine with the specified base path.
     *
     * @param basePath         The base path for all dependencies to be downloaded.
     * @param addDefaultLoader Whether or not to include the default dependency loaders.
     * @return A new {@link DependencyEngine}.
     */
    @Contract("_,_ -> new")
    public static @NotNull DependencyEngine createNew(
        final @NotNull Path basePath,
        final boolean addDefaultLoader
    ) {
        return DependencyEngine.createNew(basePath, addDefaultLoader, Throwable::printStackTrace);
    }

    /**
     * Creates a new Dependency Engine with the specified base path.
     *
     * @param basePath          The base path for all dependencies to be downloaded.
     * @param addDefaultLoader  Whether or not to include the default dependency loaders.
     * @param exceptionConsumer An error handling consumer, used if any exceptions occur during dependency loading.
     * @return A new {@link DependencyEngine}.
     */
    @Contract("_,_,_ -> new")
    public static @NotNull DependencyEngine createNew(
        final @NotNull Path basePath,
        final boolean addDefaultLoader,
        final @NotNull Consumer<@NotNull DependencyLoadException> exceptionConsumer
    ) {
        final DependencyEngine engine = new DependencyEngine(basePath);

        if (!addDefaultLoader) {
            return engine;
        }

        try {
            engine.addDefaultDependencyLoaders();
            return engine;
        } catch (Exception e) {
            exceptionConsumer.accept(new DependencyLoadException(e));
            return engine;
        }
    }

    private void addDefaultDependencyLoaders() {
        this.addDependencyLoader(MavenDependencyLoader.class, new MavenDependencyLoader(this.basePath));
    }

    /**
     * Adds a new dependency loader to the dependency engine.
     *
     * @param clazz            The class type of the dependency loader.
     * @param dependencyLoader The instance of the dependency loader.
     * @param <T>              The generic type of the dependency loader.
     */
    public <@NotNull T extends DependencyLoader<@NotNull ?>> void addDependencyLoader(
        final @NotNull Class<@NotNull ? extends T> clazz,
        final @NotNull T dependencyLoader
    ) {
        this.dependencyLoaders.put(clazz, dependencyLoader);
    }

    /**
     * Adds a new dependency loader the dependency engine.
     *
     * @param dependencyLoader The dependency loader instance.
     */
    public void addDependencyLoader(final @NotNull DependencyLoader<@NotNull ?> dependencyLoader) {
        this.addDependencyLoader(dependencyLoader.getClass(), dependencyLoader);
    }

    private @NotNull CompletableFuture<Void> loadAllDependencies(
        final @NotNull Object object,
        final @NotNull Executor executor
    ) {
        return CompletableFuture.runAsync(() -> {
            final AtomicReference<Optional<Exception>> exceptionReference = new AtomicReference<>(Optional.empty());

            this.dependencyLoaders
                .values()
                .stream()
                .sorted(Comparator.comparingInt(loader -> loader.getClass()
                                                                .getAnnotation(LoaderPriority.class)
                                                                .value()))
                .forEach(loader -> {
                    if (exceptionReference.get().isPresent()) {
                        return;
                    }

                    loader.loadDependenciesFrom(object);

                    try {
                        loader.downloadDependencies();
                        if (loader instanceof RelocatableDependencyLoader) {
                            ((RelocatableDependencyLoader<?>) loader).relocateDependencies();
                        }
                        loader.loadDependencies((URLClassLoader) this.getClass().getClassLoader());
                    } catch (Exception e) {
                        exceptionReference.set(Optional.of(e));
                    }
                });

            final Optional<Exception> exception = exceptionReference.get();

            if (exception.isPresent()) {
                throw new DependencyLoadException(exception.get());
            }
        }, executor);
    }

    /**
     * Loads all the dependencies inside the specified executor.
     *
     * @param clazz    The class to load all the dependencies for.
     * @param executor The executor to load all the dependencies in.
     * @return A CompletableFuture that completes after all dependencies have been loaded for the class.
     */
    @Contract("_,_ -> new")
    public @NotNull CompletableFuture<Void> loadAllDependencies(
        final @NotNull Class<@NotNull ?> clazz,
        final @NotNull Executor executor
    ) {
        return this.loadAllDependencies((Object) clazz, executor);
    }

    /**
     * Loads all dependencies inside of {@link ForkJoinPool#commonPool()}.
     *
     * @param clazz The class to load the dependencies from.
     * @return A CompletableFuture that completes after all dependencies have been loaded for the class.
     */
    @Contract("_ -> new")
    public @NotNull CompletableFuture<Void> loadAllDependencies(final @NotNull Class<@NotNull ?> clazz) {
        return this.loadAllDependencies(clazz, ForkJoinPool.commonPool());
    }

    /**
     * Loads all dependencies inside the specified executor.
     *
     * @param provider The dependency provider to load the dependencies from.
     * @param executor The executor to load all the dependencies in.
     * @return A CompletableFuture that completes after all dependencies have been loaded for the class.
     */
    public @NotNull CompletableFuture<Void> loadAllDependencies(
        final @NotNull DependencyProvider<?> provider,
        final @NotNull Executor executor
    ) {
        return this.loadAllDependencies((Object) provider, executor);
    }

    /**
     * Loads all dependencies inside of {@link ForkJoinPool#commonPool()}.
     *
     * @param provider The dependency provider to load the dependencies from.
     * @return A CompletableFuture that completes after all dependencies have been loaded for the class.
     */
    public @NotNull CompletableFuture<Void> loadAllDependencies(
        final @NotNull DependencyProvider<?> provider
    ) {
        return this.loadAllDependencies(provider, ForkJoinPool.commonPool());
    }
}