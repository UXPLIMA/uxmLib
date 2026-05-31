/**
 * A Folia-ready scheduling abstraction. {@link com.uxplima.uxmlib.scheduler.Scheduler} hides the four
 * Paper schedulers (global / region / entity / async) behind one interface so plugin code never touches
 * {@code BukkitScheduler} and runs unchanged on Folia. Every method returns a cancellable
 * {@link com.uxplima.uxmlib.scheduler.TaskHandle}.
 */
@NullMarked
package com.uxplima.uxmlib.scheduler;

import org.jspecify.annotations.NullMarked;
