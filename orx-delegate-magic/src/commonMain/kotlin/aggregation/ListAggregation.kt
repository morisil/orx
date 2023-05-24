@file:Suppress("PackageDirectoryMismatch")

package org.openrndr.extra.delegatemagic.aggregation

import org.openrndr.Clock
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

/**
 * Property delegation by list aggregation
 */
class ListPropertyAggregation<T, R>(
    private val clock: Clock,
    private val property: KProperty0<List<T>>,
    val aggregationFunction: (List<T>) -> R
) {
    private var output: R? = null
    private var lastTime: Double? = null

    operator fun getValue(any: Any?, property: KProperty<*>): R {
        if (lastTime != null) {
            val dt = clock.seconds - lastTime!!
            if (dt > 1E-10) {
                output = aggregationFunction(this.property.get())
            }
        } else {
            output = aggregationFunction(this.property.get())
        }

        lastTime = clock.seconds
        return output!!
    }
}

/**
 * Aggregate list property
 * @param property the list property to aggregate
 * @param aggregationFunction the function that is
 * @since 0.4.3
 */
fun <T, R> Clock.aggregating(
    property: KProperty0<List<T>>,
    aggregationFunction: (List<T>) -> R
): ListPropertyAggregation<T, R> {
    return ListPropertyAggregation(this, property, aggregationFunction)
}