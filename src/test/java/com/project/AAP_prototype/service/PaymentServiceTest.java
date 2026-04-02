package com.project.AAP_prototype.service;

import com.project.AAP_prototype.entity.Payment;
import com.project.AAP_prototype.exception.DuplicatePaymentException;
import com.project.AAP_prototype.repository.PaymentRepository;
import com.project.AAP_prototype.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    NotificationService notificationService;

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    PaymentService paymentService;

    @Mock
    HttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "apiKey", "test-key");
        ReflectionTestUtils.setField(paymentService, "apiSecret", "test-secret");

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getRequestURI()).thenReturn("/api/payments/verify");
    }

    @Test
    @DisplayName("처음 들어온 주문번호는 정상 저장된다")
    void 처음_들어온_주문번호는_정상_저장된다() throws Exception {
        // given
        String merchantUid = "order_1000";
        Payment saved = new Payment();
        saved.setOrderId(merchantUid);
        saved.setAmount(1000L);
        saved.setStatus("PAID");

        when(paymentRepository.existsByOrderId(merchantUid)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        // when
        Payment result = paymentService.verifyAndSave("imp_123", merchantUid, 1000L, "홍길동", httpRequest);

        // then
        assertThat(result.getOrderId()).isEqualTo(merchantUid);
        assertThat(result.getStatus()).isEqualTo("PAID");
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("동일한 주문번호로 두 번 요청하면 DuplicatePaymentException이 발생한다")
    void 동일한_주문번호로_두번_요청하면_예외가_발생한다() {
        // given
        String merchantUid = "order_1000";
        when(paymentRepository.existsByOrderId(merchantUid)).thenReturn(true);

        // when & then
        assertThatThrownBy(() ->
            paymentService.verifyAndSave("imp_456", merchantUid, 1000L, "홍길동", httpRequest))
            .isInstanceOf(DuplicatePaymentException.class)
            .hasMessageContaining(merchantUid);

        verify(paymentRepository, never()).save(any());
    }
}
