import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        // 💥 1. 재고 10개짜리 상품에 100명이 동시에 들이박음
        burst_traffic_attack: {
            executor: 'shared-iterations',
            vus: 100,
            iterations: 100,
            exec: 'attackStock',
        },
        // 💥 2. 1명이 프론트 렉을 뚫고 5번을 0.0001초 만에 연속 클릭함
        double_submit_attack: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 1,
            exec: 'attackDoubleSubmit',
            startTime: '5s',
        },
    },
};

const BASE_URL = 'http://localhost:58181/api/orders';

// [오픈런 공격]
export function attackStock() {
    const payload = JSON.stringify({
        userId: __VU,
        productId: 777,
        // 💡 각 유저마다 다른 주문 번호 부여
        requestId: 'openrun-req-' + __VU + '-' + __ITER
    });

    const res = http.post(BASE_URL, payload, { headers: { 'Content-Type': 'application/json' } });

    // 💡 팩폭: 낙관적 락이 작동하면, 10명만 200 OK고 90명은 500 에러(OptimisticLockingFailureException)를 맞아야 정상!
    check(res, {
        'Open Run: Handled correctly (200 or 500)': (r) => r.status === 200 || r.status === 500
    });
}

// [따닥 중복결제 공격]
export function attackDoubleSubmit() {
    const reqs = [];
    // 💡 핵심: 5번의 요청이 전부 '똑같은 주문 번호'를 들고 옴
    const sameRequestId = 'double-submit-uuid-9999';

    const payload = JSON.stringify({
        userId: 9999,
        productId: 888,
        requestId: sameRequestId
    });

    const params = { headers: { 'Content-Type': 'application/json' } };

    // 5개의 요청을 장전
    for (let i = 0; i < 5; i++) {
        reqs.push(['POST', BASE_URL, payload, params]);
    }

    // 0.0001초 오차 없이 동시 발사
    const responses = http.batch(reqs);

    // 💡 팩폭: 5개 중 딱 1개만 200 OK가 떨어지고, 4개는 500 에러(DuplicateKeyException)가 떠야 정상 방어된 거임!
    let successCount = responses.filter(r => r.status === 200).length;
    check(successCount, {
        'Double Submit: EXACTLY 1 success out of 5': (s) => s === 1
    });
}