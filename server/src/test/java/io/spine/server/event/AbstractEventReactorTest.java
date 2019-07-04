/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.event;

import com.google.common.collect.ImmutableList;
import io.spine.server.event.given.AbstractReactorTestEnv.AutoCharityDonor;
import io.spine.server.event.given.AbstractReactorTestEnv.RestaurantNotifier;
import io.spine.server.event.given.AbstractReactorTestEnv.ServicePerformanceTracker;
import io.spine.testing.client.blackbox.Count;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.event.given.AbstractReactorTestEnv.someOrderPaidFor;
import static io.spine.server.event.given.AbstractReactorTestEnv.someOrderReady;
import static io.spine.server.event.given.AbstractReactorTestEnv.someOrderServedInTime;
import static io.spine.server.event.given.AbstractReactorTestEnv.someOrderServedLate;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Abstract event reactor should")
class AbstractEventReactorTest {

    private SingleTenantBlackBoxContext restaurantContext;
    private SingleTenantBlackBoxContext deliveryContext;
    private SingleTenantBlackBoxContext charityContext;

    private AutoCharityDonor charityDonor;
    private ServicePerformanceTracker performanceTracker;

    @BeforeEach
    void setUp() {
        restaurantContext = BlackBoxBoundedContext.singleTenant();
        deliveryContext = BlackBoxBoundedContext.singleTenant();
        charityContext = BlackBoxBoundedContext.singleTenant();

        charityDonor = new AutoCharityDonor(charityContext.eventBus());
        charityContext.withEventDispatchers(charityDonor);

        performanceTracker = new ServicePerformanceTracker(restaurantContext.eventBus());
        RestaurantNotifier notifier = new RestaurantNotifier(restaurantContext.eventBus());
        restaurantContext.withEventDispatchers(performanceTracker, notifier);
    }

    @Test
    @DisplayName("throw upon a null event bus")
    void throwOnNullEventBus() {
        assertThrows(NullPointerException.class, () -> new AutoCharityDonor(null));
    }

    @DisplayName("while dealing with domestic events")
    @Nested
    class DomesticEvents {

        @Test
        @DisplayName("receive one")
        void receive() {
            OrderServed orderServed = someOrderServedInTime();
            restaurantContext.receivesEvent(orderServed);
            ImmutableList<OrderServed> ordersServed = performanceTracker.ordersServed();
            ImmutableList<OrderServedLate> ordersServedLate = performanceTracker.ordersServedLate();
            assertThat(ordersServed).containsExactly(orderServed);
            assertThat(ordersServedLate).isEmpty();
        }

        @Test
        @DisplayName("receive several")
        void receiveSeveral() {
            ImmutableList<OrderServed> eventsToEmit = ImmutableList.of(
                    someOrderServedInTime(), someOrderServedInTime(), someOrderServedLate()
            );
            eventsToEmit.forEach(restaurantContext::receivesEvent);
            ImmutableList<OrderServed> ordersServed = performanceTracker.ordersServed();
            ImmutableList<OrderServedLate> ordersServedLate = performanceTracker.ordersServedLate();
            assertThat(ordersServed).hasSize(eventsToEmit.size());
            long ordersInTime = ordersServed
                    .stream()
                    .filter(orderServed -> !performanceTracker.servedLate(orderServed))
                    .count();
            assertThat(ordersInTime).isEqualTo(2);
            assertThat(ordersServedLate).hasSize(1);
        }

        @Test
        @DisplayName("react with one")
        void react() {
            OrderServed servedLate = someOrderServedLate();
            restaurantContext.receivesEvent(servedLate)
                             .assertThat(emittedEvent(OrderServedLate.class, once()));
        }

        @Test
        @DisplayName("react with none")
        void reactWithNone() {
            OrderServed orderServed = someOrderServedInTime();
            restaurantContext.receivesEvent(orderServed)
                             .assertThat(emittedEvent(OrderServedLate.class, Count.none()));
        }

        @Test
        @DisplayName("react with several events")
        void reactWithSeveral() {
            OrderReadyToBeServed orderIsReady = someOrderReady();
            restaurantContext.receivesEvent(orderIsReady)
                             .assertThat(emittedEvent(CustomerNotified.class, once()))
                             .assertThat(emittedEvent(DeliveryServiceNotified.class, once()));
        }
    }

    @DisplayName("while dealing with external events")
    @Nested
    class ExternalEvents {

        @DisplayName("receive one")
        @Test
        void receive() {
            OrderPaidFor orderPaidFor = someOrderPaidFor();
            charityContext.receivesExternalEvent(restaurantContext.name(), orderPaidFor);

            double orderCost = orderPaidFor.getOrder()
                                           .getPriceInUsd();
            double expectedDonationAmount = orderCost * 0.02;
            assertThat(charityDonor.totalDonated()).isEqualTo(expectedDonationAmount);
        }

        @DisplayName("react to one")
        @Test
        void reactToOne() {
            OrderPaidFor orderPaidFor = someOrderPaidFor();
            charityContext.receivesExternalEvent(deliveryContext.name(), orderPaidFor)
                          .assertThat(emittedEvent(DonationMade.class, once()));
        }

        @DisplayName("react to several")
        @Test
        void reactToSeveral() {
            OrderPaidFor paidInRestaurant = someOrderPaidFor();
            OrderPaidFor paidToDelivery = someOrderPaidFor();

            charityContext.receivesExternalEvent(deliveryContext.name(), paidToDelivery)
                          .receivesExternalEvent(restaurantContext.name(), paidInRestaurant)
                          .assertThat(emittedEvent(DonationMade.class, twice()));
        }
    }
}
