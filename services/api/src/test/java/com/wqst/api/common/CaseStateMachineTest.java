package com.wqst.api.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class CaseStateMachineTest {
    private final CaseStateMachine machine=new CaseStateMachine();

    @Test void acceptsEveryDeclaredTransition(){
        assertThatCode(()->machine.check("DRAFT","PENDING_REVIEW")).doesNotThrowAnyException();
        assertThatCode(()->machine.check("PENDING_REVIEW","NEED_SUPPLEMENT")).doesNotThrowAnyException();
        assertThatCode(()->machine.check("PENDING_REVIEW","PROCESSING")).doesNotThrowAnyException();
        assertThatCode(()->machine.check("PENDING_REVIEW","CANCELED")).doesNotThrowAnyException();
        assertThatCode(()->machine.check("NEED_SUPPLEMENT","PENDING_REVIEW")).doesNotThrowAnyException();
        assertThatCode(()->machine.check("PROCESSING","COMPLETED")).doesNotThrowAnyException();
        assertThatCode(()->machine.check("PROCESSING","NEED_SUPPLEMENT")).doesNotThrowAnyException();
    }

    @Test void rejectsBypassAndTerminalTransitions(){
        assertThatThrownBy(()->machine.check("DRAFT","COMPLETED")).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("CASE_STATUS_TRANSITION_DENIED");
        assertThatThrownBy(()->machine.check("COMPLETED","PROCESSING")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->machine.check("CANCELED","PENDING_REVIEW")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->machine.check("UNKNOWN","DRAFT")).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("CASE_STATUS_INVALID");
    }
}
