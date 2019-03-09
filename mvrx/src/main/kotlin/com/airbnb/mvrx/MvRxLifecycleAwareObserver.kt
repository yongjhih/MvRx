package com.airbnb.mvrx

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Lifecycle.Event
import android.arch.lifecycle.Lifecycle.State
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.internal.disposables.DisposableHelper
import io.reactivex.internal.functions.Functions
import io.reactivex.internal.observers.LambdaObserver
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * A wrapper around an [Observer] associated with a [LifecycleOwner]. It has an [activeState], and when in a lifecycle state greater
 * than the [activeState] (as defined by [Lifecycle.State.isAtLeast()]) it will deliver values to the [sourceObserver] or [onNext] lambda.
 * When in a lower lifecycle state, the most recent update will be saved, and delivered when active again.
 */
internal class MvRxLifecycleAwareObserver<T : Any>(
    private var owner: LifecycleOwner?,
    lastDeliveredValue: T?,
    private val onDeliver: (T) -> Unit,
    private val activeState: Lifecycle.State = DEFAULT_ACTIVE_STATE,
    private val alwaysDeliverLastValueWhenUnlocked: Boolean = false,
    private var sourceObserver: Observer<T>?
) : AtomicReference<Disposable>(), LifecycleObserver, Observer<T>, Disposable {

    constructor(
        owner: LifecycleOwner,
        lastDeliveredValue: T?,
        onDeliver: (T) -> Unit,
        activeState: Lifecycle.State = DEFAULT_ACTIVE_STATE,
        alwaysDeliverLastValueWhenUnlocked: Boolean = false,
        onComplete: Action = Functions.EMPTY_ACTION,
        onSubscribe: Consumer<in Disposable> = Functions.emptyConsumer(),
        onError: Consumer<in Throwable> = Functions.ON_ERROR_MISSING,
        onNext: Consumer<T> = Functions.emptyConsumer()
    ) : this(owner, lastDeliveredValue, onDeliver, activeState, alwaysDeliverLastValueWhenUnlocked, LambdaObserver<T>(onNext, onError, onComplete, onSubscribe))

    private var deliveredFirstValue = AtomicBoolean(false)
    private var lastUndeliveredValue: T? = null
    private var lastValue: T? = lastDeliveredValue
    private val locked = AtomicBoolean(true)
    private val isUnlocked get() = !locked.get()

    override fun onSubscribe(d: Disposable) {
        if (DisposableHelper.setOnce(this, d)) {
            requireOwner().lifecycle.addObserver(this)
            requireSourceObserver().onSubscribe(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        requireOwner().lifecycle.removeObserver(this)
        if (!isDisposed) {
            dispose()
        }
        owner = null
        sourceObserver = null
    }

    @OnLifecycleEvent(Event.ON_ANY)
    fun onLifecycleEvent() {
        updateLock()
    }

    private fun updateLock() {
        if (owner?.lifecycle?.currentState?.isAtLeast(activeState) == true) {
            unlock()
        } else {
            lock()
        }
    }

    override fun onNext(value: T) {
        if (isUnlocked) {
          // Don't emit on the first value we deliver, if we had already delivered this value before AND
          // the observer is set to not always deliver when unlocked.
          val ignoreValue = !deliveredFirstValue.getAndSet(true) && !alwaysDeliverLastValueWhenUnlocked && value == lastValue
          if (!ignoreValue) {
            requireSourceObserver().onNext(value)
            onDeliver(value)
          }
        } else {
            lastUndeliveredValue = value
        }
        lastValue = value
    }

    override fun onError(e: Throwable) {
        if (!isDisposed) {
            lazySet(DisposableHelper.DISPOSED)
            requireSourceObserver().onError(e)
        }
    }

    override fun onComplete() {
        requireSourceObserver().onComplete()
    }

    override fun dispose() {
        DisposableHelper.dispose(this)
    }

    override fun isDisposed(): Boolean {
        return get() === DisposableHelper.DISPOSED
    }

    private fun unlock() {
        if (!locked.getAndSet(false)) {
            return
        }
        if (!isDisposed) {
            val valueToDeliverOnUnlock = if (alwaysDeliverLastValueWhenUnlocked && lastValue != null) lastValue else lastUndeliveredValue
            lastUndeliveredValue = null
            if (valueToDeliverOnUnlock != null) {
                onNext(valueToDeliverOnUnlock)
            }
        }
    }

    private fun lock() {
        locked.set(true)
    }

    private fun requireOwner(): LifecycleOwner = requireNotNull(owner) { "Cannot access lifecycleOwner after onDestroy." }

    private fun requireSourceObserver() = requireNotNull(sourceObserver) { "Cannot access observer after onDestroy." }

    companion object {
        private val DEFAULT_ACTIVE_STATE = State.STARTED
    }
}