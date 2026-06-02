package com.uxplima.uxmlib.condition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.Test;

/** AND-combination, failure-message flushing, cancel raising, and stop-chain short-circuit. */
class ConditionListTest {

    private static final Condition PASS = request -> true;
    private static final Condition FAIL = request -> false;

    private static ConditionRequest request() {
        return ConditionRequest.builder(OperandResolver.identity()).build();
    }

    @Test
    void allPassingReturnsTrueAndCollectsNoErrors() {
        ConditionRequest request = request();
        ConditionList list = ConditionList.builder()
                .require(PASS, Component.text("a"))
                .require(PASS, Component.text("b"))
                .build();
        assertThat(list.test(request)).isTrue();
        assertThat(request.errors()).isEmpty();
        assertThat(request.isCancelled()).isFalse();
    }

    @Test
    void aSingleFailureReturnsFalseAndRecordsItsMessage() {
        ConditionRequest request = request();
        ConditionList list = ConditionList.builder()
                .require(PASS, Component.text("a"))
                .require(FAIL, Component.text("b failed"))
                .build();
        assertThat(list.test(request)).isFalse();
        assertThat(request.errors()).hasSize(1);
        assertThat(com.uxplima.uxmlib.text.Text.plain(request.errors().get(0))).isEqualTo("b failed");
    }

    @Test
    void byDefaultEveryFailureIsCollectedNotShortCircuited() {
        ConditionRequest request = request();
        ConditionList list = ConditionList.builder()
                .require(FAIL, Component.text("first"))
                .require(FAIL, Component.text("second"))
                .build();
        assertThat(list.test(request)).isFalse();
        assertThat(request.errors()).hasSize(2);
    }

    @Test
    void silencePolicyFailsWithoutRecordingAMessage() {
        ConditionRequest request = request();
        ConditionList list = ConditionList.builder().requireSilent(FAIL).build();
        assertThat(list.test(request)).isFalse();
        assertThat(request.errors()).isEmpty();
    }

    @Test
    void cancelPolicyRaisesTheRequestCancelFlag() {
        ConditionRequest request = request();
        ConditionList list = ConditionList.builder()
                .add(FAIL, FailurePolicy.CANCEL, Component.text("blocked"))
                .build();
        assertThat(list.test(request)).isFalse();
        assertThat(request.isCancelled()).isTrue();
        assertThat(request.errors()).hasSize(1);
    }

    @Test
    void stopChainHaltsEvaluationOfLaterEntries() {
        AtomicInteger laterEvaluations = new AtomicInteger();
        Condition counting = req -> {
            laterEvaluations.incrementAndGet();
            return true;
        };
        ConditionRequest request = request();
        ConditionList list = ConditionList.builder()
                .add(FAIL, FailurePolicy.STOP_CHAIN, Component.text("gate"))
                .require(counting, Component.text("never reached"))
                .build();
        assertThat(list.test(request)).isFalse();
        assertThat(request.errors()).hasSize(1);
        assertThat(laterEvaluations.get()).isZero();
    }

    @Test
    void laterEntriesStillRunWhenAnEarlierFailureDoesNotStopTheChain() {
        AtomicInteger evaluations = new AtomicInteger();
        Condition counting = req -> {
            evaluations.incrementAndGet();
            return false;
        };
        ConditionRequest request = request();
        ConditionList list = ConditionList.builder()
                .require(FAIL, Component.text("first"))
                .require(counting, Component.text("second"))
                .build();
        assertThat(list.test(request)).isFalse();
        assertThat(evaluations.get()).isEqualTo(1);
        assertThat(request.errors()).hasSize(2);
    }

    @Test
    void emptyListPasses() {
        ConditionRequest request = request();
        assertThat(ConditionList.builder().build().test(request)).isTrue();
    }
}
