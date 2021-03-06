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

package com.heretere.hdl.spigot.builder;

import com.heretere.hdl.dependency.builder.DependencyProvider;
import com.heretere.hdl.spigot.SpigotDependencyInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Used to be attached to a {@link com.heretere.hdl.spigot.SpigotDependencyLoader} stores spigot dependencies.
 */
public final class SpigotDependencyProvider implements DependencyProvider<SpigotDependencyInfo> {
    /**
     * The dependencies contained in this dependency provider.
     */
    private final @NotNull Set<@NotNull SpigotDependencyInfo> dependencies;

    SpigotDependencyProvider(final @NotNull Set<@NotNull SpigotDependencyInfo> dependencies) {
        this.dependencies = dependencies;
    }

    @Override public @NotNull Set<@NotNull SpigotDependencyInfo> getDependencies() {
        return this.dependencies;
    }

    /**
     * Creates a new builder instance.
     *
     * @return A new {@link SpigotDependencyProviderBuilder} instance.
     */
    @Contract("-> new")
    public static SpigotDependencyProviderBuilder builder() {
        return new SpigotDependencyProviderBuilder();
    }

    @Override public @NotNull Class<SpigotDependencyInfo> getGenericType() {
        return SpigotDependencyInfo.class;
    }
}
