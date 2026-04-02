package com.project.AAP_prototype.exception;

public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String merchantUid) {
        super("이미 처리된 주문입니다: " + merchantUid);
    }
}
