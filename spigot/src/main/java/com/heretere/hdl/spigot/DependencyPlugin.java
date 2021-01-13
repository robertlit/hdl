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

package com.heretere.hdl.spigot;

import com.heretere.hdl.DependencyEngine;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Used to automatically load dependencies for a plugin.
 * It extends JavaPlugin.
 */
public abstract class DependencyPlugin extends JavaPlugin {
    /**
     * The dependency engine instance.
     */
    private final @NotNull DependencyEngine dependencyEngine;

    /**
     * Indicates whether or not dependencies were successfully loaded.
     */
    private final @NotNull AtomicBoolean hasError = new AtomicBoolean(false);

    protected DependencyPlugin() {
        this.dependencyEngine = DependencyEngine.createNew(this.getDataFolder().toPath().resolve("dependencies"));
    }

    @Override public final void onLoad() {
        super.onLoad();

        this.dependencyEngine.loadAllDependencies(this.getClass())
                             .exceptionally(e -> {
                                 this.getLogger().log(Level.SEVERE, "An error occurred while loading dependencies", e);
                                 this.hasError.set(true);
                                 return null;
                             })
                             .join();

        if (!this.hasError.get()) {
            this.load();
        }
    }

    @Override public final void onEnable() {
        super.onEnable();

        if (!this.hasError.get()) {
            this.enable();
        }
    }

    @Override public final void onDisable() {
        super.onDisable();

        if (!this.hasError.get()) {
            this.disable();
        }
    }

    protected abstract void load();

    protected abstract void enable();

    protected abstract void disable();

    /**
     * @return The current dependency engine instance for this dependency plugin.
     */
    public @NotNull DependencyEngine getDependencyEngine() {
        return this.dependencyEngine;
    }
}