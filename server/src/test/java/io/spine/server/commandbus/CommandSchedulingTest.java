/*
 * Copyright 2018, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.commandbus;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.testing.Tests;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.time.Durations2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.CommandStatus.SCHEDULED;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.commandbus.CommandScheduler.setSchedule;
import static io.spine.server.commandbus.Given.ACommand.addTask;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.server.commandbus.Given.ACommand.startProject;
import static io.spine.time.Durations2.minutes;
import static io.spine.time.testing.TimeTests.Past.minutesAgo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("Command scheduling mechanism should")
class CommandSchedulingTest extends AbstractCommandBusTestSuite {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandSchedulingTest.class);

    CommandSchedulingTest() {
        super(true);
    }

    @Test
    @DisplayName("store scheduled command to CommandStore and return `OK`")
    void storeScheduledCommand() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, observer);

        verify(commandStore).store(cmd, SCHEDULED);
        checkResult(cmd);
    }

    @Test
    @DisplayName("schedule command if delay is set")
    void scheduleIfDelayIsSet() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, observer);

        verify(scheduler).schedule(cmd);
    }

    @Test
    @DisplayName("not schedule command if no scheduling options are set")
    void notScheduleWithoutOptions() {
        commandBus.register(new CreateProjectHandler());

        final Command command = createProject();
        commandBus.post(command, observer);

        verify(scheduler, never()).schedule(createProject());
        checkResult(command);
    }

    @Test
    @DisplayName("reschedule commands from storage")
    void rescheduleCmdsFromStorage() {
        final Timestamp schedulingTime = minutesAgo(3);
        final Duration delayPrimary = Durations2.fromMinutes(5);
        final Duration newDelayExpected = Durations2.fromMinutes(2); // = 5 - 3
        final List<Command> commandsPrimary = newArrayList(createProject(),
                                                           addTask(),
                                                           startProject());
        storeAsScheduled(commandsPrimary, delayPrimary, schedulingTime);

        commandBus.rescheduleCommands();

        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(commandsPrimary.size())).schedule(commandCaptor.capture());
        final List<Command> commandsRescheduled = commandCaptor.getAllValues();
        for (Command cmd : commandsRescheduled) {
            final long actualDelay = getDelaySeconds(cmd);
            Tests.assertSecondsEqual(newDelayExpected.getSeconds(), actualDelay, /*maxDiffSec=*/1);
        }
    }

    @Nested
    @DisplayName("reschedule commands from storage on build")
    class RescheduleOnBuild {

        @SuppressWarnings("CheckReturnValue")
        // OK to ignore stored command for the purpose of this test.
        @Test
        @DisplayName("in parallel if thread spawning is allowed")
        void inParallel() {
            final String mainThreadName = Thread.currentThread().getName();
            final StringBuilder threadNameUponScheduling = new StringBuilder(0);
            final CountDownLatch latch = new CountDownLatch(1);
            final CommandScheduler scheduler =
                    threadAwareScheduler(threadNameUponScheduling, latch);
            storeSingleCommandForRescheduling();

            // Create CommandBus specific for this test.
            final CommandBus commandBus = CommandBus.newBuilder()
                                                    .setCommandStore(commandStore)
                                                    .setCommandScheduler(scheduler)
                                                    .setThreadSpawnAllowed(true)
                                                    .setAutoReschedule(true)
                                                    .build();
            assertNotNull(commandBus);

            // Await to ensure the commands have been rescheduled in parallel.
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }

            // Ensure the scheduler has been called for a single command,
            final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
            verify(scheduler, times(1)).schedule(commandCaptor.capture());

            // and the call has been made for a thread, different than the main thread.
            final String actualThreadName = threadNameUponScheduling.toString();
            assertNotNull(actualThreadName);
            assertNotEquals(mainThreadName, actualThreadName);
        }

        @SuppressWarnings("CheckReturnValue")
        // OK to ignore stored command for the purpose of this test.
        @Test
        @DisplayName("synchronously if thread spawning is not allowed")
        void synchronously() {
            final String mainThreadName = Thread.currentThread().getName();
            final StringBuilder threadNameUponScheduling = new StringBuilder(0);
            final CountDownLatch latch = new CountDownLatch(1);
            final CommandScheduler scheduler =
                    threadAwareScheduler(threadNameUponScheduling, latch);
            storeSingleCommandForRescheduling();

            // Create CommandBus specific for this test.
            final CommandBus commandBus = CommandBus.newBuilder()
                                                    .setCommandStore(commandStore)
                                                    .setCommandScheduler(scheduler)
                                                    .setThreadSpawnAllowed(false)
                                                    .setAutoReschedule(true)
                                                    .build();
            assertNotNull(commandBus);

            // Ensure the scheduler has been called for a single command,
            final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
            verify(scheduler, times(1)).schedule(commandCaptor.capture());

            // and the call has been made in the main thread (as spawning is not allowed).
            final String actualThreadName = threadNameUponScheduling.toString();
            assertNotNull(actualThreadName);
            assertEquals(mainThreadName, actualThreadName);
        }
    }

    @Test
    @DisplayName("post previously scheduled command")
    void postPreviouslyScheduled() {
        CommandBus spy = spy(commandBus);
        spy.register(createProjectHandler);
        Command command = storeSingleCommandForRescheduling();

        spy.postPreviouslyScheduled(command);

        verify(spy).dispatch(eq(CommandEnvelope.of(command)));
    }

    @Test
    @DisplayName("reject previously scheduled command if no endpoint is found")
    void rejectPreviouslyScheduledWithoutEndpoint() {
        Command command = storeSingleCommandForRescheduling();
        assertThrows(IllegalStateException.class,
                     () -> commandBus.postPreviouslyScheduled(command));
    }

    @Nested
    @DisplayName("allow updating")
    class Update {

        @Test
        @DisplayName("scheduling options")
        void schedulingOptions() {
            final Command cmd = requestFactory.command()
                                              .create(toMessage(newUuid()));
            final Timestamp schedulingTime = getCurrentTime();
            final Duration delay = Durations2.minutes(5);

            final Command cmdUpdated = setSchedule(cmd, delay, schedulingTime);
            final CommandContext.Schedule schedule = cmdUpdated.getContext()
                                                               .getSchedule();

            assertEquals(delay, schedule.getDelay());
            assertEquals(schedulingTime, cmdUpdated.getSystemProperties()
                                                   .getSchedulingTime());
        }

        @Test
        @DisplayName("scheduling time")
        void schedulingTime() {
            final Command cmd = requestFactory.command()
                                              .create(toMessage(newUuid()));
            final Timestamp schedulingTime = getCurrentTime();

            final Command cmdUpdated = CommandScheduler.setSchedulingTime(cmd, schedulingTime);

            assertEquals(schedulingTime, cmdUpdated.getSystemProperties()
                                                   .getSchedulingTime());
        }
    }

    /*
     * Utility methods
     ********************/

    private static Command createScheduledCommand() {
        final Timestamp schedulingTime = minutesAgo(3);
        final Duration delayPrimary = Durations2.fromMinutes(5);
        return setSchedule(createProject(), delayPrimary, schedulingTime);
    }

    /**
     * Creates and stores one scheduled command.
     */
    private Command storeSingleCommandForRescheduling() {
        final Command cmdWithSchedule = createScheduledCommand();
        commandStore.store(cmdWithSchedule, SCHEDULED);
        return cmdWithSchedule;
    }

    /**
     * Creates a new thread-aware scheduler spied by Mockito.
     *
     * <p>The method is not {@code static} to allow Mockito spy on the created anonymous class
     * instance.
     *
     * @param targetThreadName the builder of the thread name that will be created upon command
     *                         scheduling
     * @param latch            the instance of the {@code CountDownLatch} to await the execution
     *                         finishing
     * @return newly created instance
     */
    @SuppressWarnings("MethodMayBeStatic") // see Javadoc.
    private CommandScheduler threadAwareScheduler(final StringBuilder targetThreadName,
                                                  final CountDownLatch latch) {
        return spy(new ExecutorCommandScheduler() {
            @Override
            public void schedule(Command command) {
                super.schedule(command);
                targetThreadName.append(Thread.currentThread()
                                              .getName());
                latch.countDown();
            }
        });
    }

    private static long getDelaySeconds(Command cmd) {
        final long delaySec = cmd
                .getContext()
                .getSchedule()
                .getDelay()
                .getSeconds();
        return delaySec;
    }
}