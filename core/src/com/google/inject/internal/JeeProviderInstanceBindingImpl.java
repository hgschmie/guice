/*
 * Copyright (C) 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.internal;

import static com.google.inject.internal.GuiceInternal.GUICE_INTERNAL;
import static com.google.inject.spi.Elements.withTrustedSource;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.HasDependencies;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.JeeProviderInstanceBinding;
import com.google.inject.spi.ProviderWithExtensionVisitor;
import java.util.Set;

class JeeProviderInstanceBindingImpl<T> extends BindingImpl<T> implements
    JeeProviderInstanceBinding<T> {

  final jakarta.inject.Provider<? extends T> providerInstance;
  final ImmutableSet<InjectionPoint> injectionPoints;

  public JeeProviderInstanceBindingImpl(
      InjectorImpl injector,
      Key<T> key,
      Object source,
      InternalFactory<? extends T> internalFactory,
      Scoping scoping,
      jakarta.inject.Provider<? extends T> providerInstance,
      Set<InjectionPoint> injectionPoints) {
    super(injector, key, source, internalFactory, scoping);
    this.providerInstance = providerInstance;
    this.injectionPoints = ImmutableSet.copyOf(injectionPoints);
  }

  public JeeProviderInstanceBindingImpl(
      Object source,
      Key<T> key,
      Scoping scoping,
      Set<InjectionPoint> injectionPoints,
      jakarta.inject.Provider<? extends T> providerInstance) {

    super(source, key, scoping);
    this.injectionPoints = ImmutableSet.copyOf(injectionPoints);
    this.providerInstance = providerInstance;
  }

  @Override
  public <V> V acceptTargetVisitor(BindingTargetVisitor<? super T, V> visitor) {
    if (providerInstance instanceof ProviderWithExtensionVisitor) {
      return ((ProviderWithExtensionVisitor<? extends T>) providerInstance)
          .acceptExtensionVisitor(visitor, this);
    } else {
      return visitor.visit(this);
    }
  }

  @Override
  public jakarta.inject.Provider<? extends T> getUserSuppliedProvider() {
    return providerInstance;
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return injectionPoints;
  }

  @Override
  public Set<Dependency<?>> getDependencies() {
    return providerInstance instanceof HasDependencies
        ? ImmutableSet.copyOf(((HasDependencies) providerInstance).getDependencies())
        : Dependency.forInjectionPoints(injectionPoints);
  }

  @Override
  public BindingImpl<T> withScoping(Scoping scoping) {
    return new JeeProviderInstanceBindingImpl<T>(
        getSource(), getKey(), scoping, injectionPoints, providerInstance);
  }

  @Override
  public BindingImpl<T> withKey(Key<T> key) {
    return new JeeProviderInstanceBindingImpl<T>(
        getSource(), key, getScoping(), injectionPoints, providerInstance);
  }

  @Override
  public void applyTo(Binder binder) {
    getScoping()
        .applyTo(
            withTrustedSource(GUICE_INTERNAL, binder, getSource())
                .bind(getKey())
                .toJeeProvider(getUserSuppliedProvider()));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(JeeProviderInstanceBinding.class)
        .add("key", getKey())
        .add("source", getSource())
        .add("scope", getScoping())
        .add("provider", providerInstance)
        .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof JeeProviderInstanceBindingImpl) {
      JeeProviderInstanceBindingImpl<?> o = (JeeProviderInstanceBindingImpl<?>) obj;
      return getKey().equals(o.getKey())
          && getScoping().equals(o.getScoping())
          && Objects.equal(providerInstance, o.providerInstance);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getKey(), getScoping());
  }
}
