package com.tterrag.registrate.util.entry;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Either;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonnullType;

import lombok.EqualsAndHashCode;
import lombok.experimental.Delegate;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wraps a {@link net.neoforged.neoforge.registries.DeferredHolder}, providing a cleaner API with null-safe access, and registrate-specific extensions such as {@link #getSibling(ResourceKey)}.
 *
 * @param <S>
 *            The type of the entry
 */
@EqualsAndHashCode(of = "delegate")
public class RegistryEntry<R, S extends R> implements NonNullSupplier<S> {
    private interface Exclusions<R, T extends R> {
        T get();
        boolean is(ResourceLocation id);
        boolean is(ResourceKey<R> key);
        boolean is(Predicate<ResourceKey<R>> filter);
        boolean is(TagKey<R> tag);
        Stream<TagKey<R>> tags();
        Either<ResourceKey<R>, R> unwrap();
        Optional<ResourceKey<R>> unwrapKey();
        boolean canSerializeIn(HolderOwner<R> owner);
    }

    private final AbstractRegistrate<?> owner;
    @Delegate(excludes = Exclusions.class)
    private final @Nullable DeferredHolder<R, S> delegate;

    @SuppressWarnings("unused")
    public RegistryEntry(AbstractRegistrate<?> owner, DeferredHolder<R, S> delegate) {
        if (owner == null)
            throw new NullPointerException("Owner must not be null");
        if (delegate == null)
            throw new NullPointerException("Delegate must not be null");
        this.owner = owner;
        this.delegate = delegate;
    }

    /**
     * Get the entry, throwing an exception if it is not present for any reason.
     *
     * @return The (non-null) entry
     */
    @Override
    public @NonnullType S get() {
        DeferredHolder<R, S> delegate = this.delegate;
        return Objects.requireNonNull(getUnchecked(), () -> delegate == null ? "Registry entry is empty" : "Registry entry not present: " + delegate.getId());
    }

    /**
     * Get the entry without performing any checks.
     *
     * @return The (nullable) entry
     */
    public @Nullable S getUnchecked() {
        DeferredHolder<R, S> delegate = this.delegate;
        return delegate == null ? null : delegate.asOptional().orElse(null);
    }

    public <X, Y extends X> RegistryEntry<X, Y> getSibling(ResourceKey<? extends Registry<X>> registryType) {
        return owner.get(getId().getPath(), registryType);
    }

    public <X, Y extends X> RegistryEntry<X,Y> getSibling(Registry<X> registry) {
        return getSibling(registry.key());
    }

    /**
     * If an entry is present, and the entry matches the given predicate, return an {@link Optional<RegistryEntry>} describing the value, otherwise return an empty {@link Optional}.
     *
     * @param predicate
     *            a {@link Predicate predicate} to apply to the entry, if present
     * @return an {@link RegistryEntry} describing the value of this {@link RegistryEntry} if the entry is present and matches the given predicate, otherwise an empty {@link RegistryEntry}
     * @throws NullPointerException
     *             if the predicate is null
     */
    public Optional<RegistryEntry<R, S>> filter(Predicate<R> predicate) {
        Objects.requireNonNull(predicate);
        if (predicate.test(get())) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    public <X> boolean is(X entry) {
        return get() == entry;
    }

    @SuppressWarnings("unchecked")
    protected static <E extends RegistryEntry<?, ?>> E cast(Class<? super E> clazz, RegistryEntry<?, ?> entry) {
        if (clazz.isInstance(entry)) {
            return (E) entry;
        }
        throw new IllegalArgumentException("Could not convert RegistryEntry: expecting " + clazz + ", found " + entry.getClass());
    }
}
