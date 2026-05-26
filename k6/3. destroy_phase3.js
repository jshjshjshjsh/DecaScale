import http from 'k6/http';
import { check } from 'k6';

// 💡 팩폭: 딱 200명이 10초 동안 무지성으로 연타하는 단일 시나리오만 셋팅!
export const options = {
    scenarios: {
        thundering_herd_attack: {
            executor: 'constant-vus',
            vus: 200,          // 200명의 가상 유저가
            duration: '10s',    // 10초 동안 쉬지 않고 연타
            exec: 'attackStockHeavy',
        },
    },
};

const BASE_URL = 'http://localhost:58181/api/retry/orders';

export function attackStockHeavy() {
    // 💡 핵심: 200명의 유저가 쏠 때마다 requestId가 완벽하게 무작위로 생성됨!
    // 그래야 유니크 인덱스(따닥) 입구컷을 통과하고 '낙관적 락 경합' 단계까지 진입함!
    const randomRequestId = 'heavy-req-' + Math.random().toString(36).substring(2) + '-' + Date.now();

    const payload = JSON.stringify({
        userId: Math.floor(Math.random() * 100000) + 1, // 무작위 유저ID
        productId: 777, // 💥 우리의 실험쥐 (재고 10개짜리 상품)
        requestId: randomRequestId
    });

    const params = { headers: { 'Content-Type': 'application/json' } };
    const res = http.post(BASE_URL, payload, params);

    check(res, { 'Status is 200': (r) => r.status === 200 });
}