package org.bubenheimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { Parent() }
    }
}

@Composable
private fun Parent() {
    var value by remember { mutableStateOf(0) }

    SideEffect { println("Parent recomposed, value: $value") }

    LaunchedEffect(Unit) {
        repeat(3) {
            delay(5_000)
            value += 1
        }
    }

    val currentValue by rememberUpdatedState(value)

    val parentComposition = rememberCompositionContext()

    LaunchedEffect(Unit) {
        disposingComposition {
            newComposition(parentComposition) {
                SideEffect { println("Subcomposition recomposed, currentValue: $currentValue") }

                Child(currentValue)
            }
        }
    }
}

@Composable
private fun Child(value: Int) {
    SideEffect { println("Child recomposed, value: $value") }

    LaunchedEffect(value) { println("Child LaunchedEffect, value: $value") }
}

private suspend inline fun disposingComposition(factory: () -> Composition) {
    val composition = factory()
    try {
        awaitCancellation()
    } finally {
        composition.dispose()
    }
}

private fun newComposition(
        parent: CompositionContext,
        content: @Composable () -> Unit
) = Composition(MyApplier(), parent).apply { setContent(content) }

private class MyNode {
    fun onAttached() {}
    fun onRemoved() {}
    fun onCleared() {}
}

private class MyApplier : AbstractApplier<MyNode>(MyNode()) {
    private val decorations = mutableListOf<MyNode>()

    override fun onClear() {
        decorations.forEach { it.onCleared() }
        decorations.clear()
    }

    override fun insertBottomUp(index: Int, instance: MyNode) {
        decorations.add(index, instance)
        instance.onAttached()
    }

    override fun insertTopDown(index: Int, instance: MyNode) {
        // insertBottomUp is preferred
    }

    override fun move(from: Int, to: Int, count: Int) {
        decorations.move(from, to, count)
    }

    override fun remove(index: Int, count: Int) {
        repeat(count) { decorations[index + it].onRemoved() }
        decorations.remove(index, count)
    }
}
