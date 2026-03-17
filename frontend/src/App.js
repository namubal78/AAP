import React, { useState, useEffect } from 'react';
import axios from 'axios'; // axios 라이브러리 사용

function App() {

  const [payments, setPayments] = useState([]); // 결제 목록 상태 관리

  // 1. 페이지 로드 시 결제 목록 가져오기
  useEffect(() => {
    fetchPayments();
  }, []);

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
        const response = await fetch('http://localhost:8080/api/payments/verify', { // 프론트엔드가 3000번 포트에서 돌아갈 때 8080번 백엔드 API를 찾을 수 있도록
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
          fetchPayments(); // 결제 성공 후 목록 갱신
        }

      } else {
        alert(`결제 실패: ${rsp.error_msg}`);
      }
    });
  };

  // 결제 목록 가져오기 함수
  const fetchPayments = async () => {
      try {
          // 상대 경로 대신 백엔드 전체 주소를 적어줍니다.
          const res = await axios.get('http://localhost:8080/api/payments/list');
          setPayments(res.data);
      } catch (err) {
          console.error("데이터 로드 실패:", err);
      }
  };

  // 환불 버튼 클릭 시 실행
  const handleRefund = async (merchantUid) => {
      if(!window.confirm("정말 환불하시겠습니까?")) return;
      
      try {
          // 백엔드의 @PostMapping("/refund") API 호출
          await axios.post('http://localhost:8080/api/payments/refund', { // 프론트엔드가 3000번 포트에서 돌아갈 때 8080번 백엔드 API를 찾을 수 있도록
              merchantUid, 
              reason: "사용자 단순 변심" 
          });
          alert("환불이 완료되었습니다.");
          fetchPayments(); // 환불 성공 후 목록 새로고침
      } catch (err) {
        alert("환불 실패: " + (err.response?.data || err.message));
      }
  };

  return (
  <div style={{ padding: '50px', textAlign: 'center' }}>
        <h1>결제 및 환불 관리 시스템</h1>
        
        {/* 결제 버튼 */}
        <button onClick={handlePayment} style={{ padding: '15px 30px', fontSize: '18px', marginBottom: '40px', cursor: 'pointer' }}>
          새로운 결제하기 (1,000원)
        </button>

        <hr />

        {/* 결제 목록 테이블 */}
        <div style={{ marginTop: '30px' }}>
          <h2>최근 결제 내역</h2>
          <table border="1" style={{ width: '100%', borderCollapse: 'collapse', marginTop: '20px' }}>
            <thead>
              <tr style={{ backgroundColor: '#f4f4f4' }}>
                <th>주문번호</th>
                <th>구매자</th>
                <th>금액</th>
                <th>상태</th>
                <th>결제일</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {payments.length > 0 ? (
                payments.map((p) => (
                  <tr key={p.id} style={{ textAlign: 'center' }}>
                    <td>{p.orderId}</td>
                    <td>{p.buyerName}</td>
                    <td>{p.amount.toLocaleString()}원</td>
                    <td style={{ color: p.status === 'CANCELLED' ? 'red' : 'blue', fontWeight: 'bold' }}>
                      {p.status === 'PAID' ? '결제완료' : '환불완료'}
                    </td>
                    <td>{new Date(p.createdAt).toLocaleString()}</td>
                    <td style={{ display: 'flex', gap: '5px', justifyContent: 'center' }}>
                      {/* 1. 환불 버튼 (기존) */}
                      {p.status === 'PAID' && (
                        <button 
                          onClick={() => handleRefund(p.orderId)}
                          style={{ backgroundColor: '#ff4d4f', color: 'white', border: 'none', padding: '5px 10px', cursor: 'pointer', borderRadius: '4px' }}
                        >
                          환불하기
                        </button>
                      )}
                      
                      {/* 2. 영수증 버튼 추가 */}
                      {p.receiptUrl ? (
                        <button 
                          onClick={() => window.open(p.receiptUrl, '_blank')}
                          style={{ backgroundColor: '#4CAF50', color: 'white', border: 'none', padding: '5px 10px', cursor: 'pointer', borderRadius: '4px' }}
                        >
                          영수증
                        </button>
                      ) : (
                        <button 
                          disabled
                          style={{ backgroundColor: '#ccc', color: 'white', border: 'none', padding: '5px 10px', borderRadius: '4px', cursor: 'not-allowed' }}
                        >
                          미발급
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" style={{ padding: '20px' }}>결제 내역이 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

export default App;