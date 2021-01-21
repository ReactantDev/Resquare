package dev.reactant.resquare.testutils

import dev.reactant.resquare.dom.Node
import dev.reactant.resquare.dom.RootContainer
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.concurrent.Executors
import java.util.stream.Stream

class TestSingleThreadRootContainer(override val content: () -> Node) : RootContainer() {
    override val updateObservable: Observable<Boolean> = rootState.updatesObservable
}

class TestMultithreadingRootContainer(override val content: () -> Node) : RootContainer() {
    private val threadPool = Executors.newFixedThreadPool(1)
    override val updateObservable: Observable<Boolean> =
        rootState.updatesObservable.observeOn(Schedulers.from(threadPool!!))

    override fun destroy() {
        updatesSubscription?.dispose()
        rootState.unmount()
        threadPool?.shutdown()
    }
}

typealias TestRootContainerFactory = (content: () -> Node) -> RootContainer

class TestRootContainerFactoryProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return Stream.of(
            { content: () -> Node -> TestSingleThreadRootContainer(content) },
            { content: () -> Node -> TestMultithreadingRootContainer(content) },
        ).map(Arguments::of)
    }
}
