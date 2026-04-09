import React from 'react';

function App() {
  const handlePayment = () => {
    const { IMP } = window;
    IMP.init('imp77614466'); // 테스트용 식별코드

    IMP.request_pay({
      pg: "kakaopay.TC0ONETIME",
      pay_method: "card",
      merchant_uid: `order_${new Date().getTime()}`,
      name: "블로그 프로젝트 결제 테스트 + 보안 강화",
      amount: 1000, // 이 금액을 백엔드에서 검증하게 됩니다.
      buyer_name: "홍길동"
    }, async (rsp) => {
      if (rsp.success) {

        // 1-1. 테스트) 이미 성공 확인하신 백엔드 API 주소로 전송!
        /*
        const response = await fetch('http://localhost:8080/api/payments/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            orderId: rsp.merchant_uid,
            amount: rsp.paid_amount,
            paymentMethod: rsp.pay_method,
            buyerName: rsp.buyer_name,
            pgProvider: rsp.pg_provider,
            status: "DONE" // 이미지에서 확인하신 완료 상태값
          })
        });

        if (response.ok) {
          alert('결제 성공 및 DB 저장 완료!');
        }
        */

        // 1-2. 디벨롭) [보안 가이드] 단순 데이터 전송 대신 고유 UID를 보내 백엔드에서 검증하게 함
        
        // 결제 성공 시 rsp 객체에 imp_uid가 생성되어 들어있습니다.
        const response = await fetch('http://localhost:8080/api/payments/verify', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            imp_uid: rsp.imp_uid,            // 결제 고유 번호
            merchant_uid: rsp.merchant_uid,  // 주문 번호
            amount: rsp.paid_amount,         // 결제 금액
            buyerName: rsp.buyer_name,       // 구매자명
            paymentMethod: rsp.pay_method    // 결제 수단
          })
        });

        if (response.ok) {
          alert('금융권 수준 사후 검증 완료!');
        }

      } else {
        alert(`결제 실패: ${rsp.error_msg}`);
      }
    });
  };

  return (
    <div style={{ textAlign: 'center', marginTop: '100px' }}>
      <h1>결제 API 모의테스트</h1>
      <button onClick={handlePayment} style={{ padding: '15px 30px', fontSize: '20px' }}>
        카카오페이로 결제하기
      </button>
    </div>
  );
}

export default App;