import http from 'k6/http';
import { check } from 'k6';

// 🚨 K6의 고급 기능인 'scenarios'와 'exec'를 써서 공격 모드를 완벽 분리
export const options = {
    scenarios: {
        // 💥 재앙 1: 수강신청 오픈런 (재고 마이너스 폭파)
        // 100명의 유저가 0.1초의 오차도 없이 동시에 결제 버튼을 누름
        burst_traffic_attack: {
            executor: 'shared-iterations',
            vus: 100,          // 100명의 가상 유저
            iterations: 100,   // 딱 100번만 실행
            exec: 'attackStock', // 아래 attackStock 함수 실행
        },

        // 💥 재앙 2: 렉걸린 유저의 미친 광클 (따닥 중복결제 폭파)
        // 1명의 유저가 프론트 렉을 뚫고 0.001초 만에 5번을 다다닥! 누름
        double_submit_attack: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 1,
            exec: 'attackDoubleSubmit', // 아래 attackDoubleSubmit 함수 실행
            startTime: '5s', // 오픈런 공격 끝나고 5초 뒤에 실행되도록 딜레이
        },
    },
};

const BASE_URL = 'http://localhost:58181/api/orders';

// ⚔️ 공격 1: 재고 마이너스 폭파 로직
export function attackStock() {
    // __VU는 1~100까지 발급되는 가상 유저 번호 (서로 다른 유저 100명)
    const payload = JSON.stringify({
        userId: __VU,
        productId: 777, // 테스트용 상품 (미리 DB에 재고 10개짜리로 세팅해둘 것!)
    });

    const res = http.post(BASE_URL, payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    // 순정 상태라면 100명 다 200 OK가 떨어지는 미친 상황이 발생할 것임
    check(res, { 'Open Run: is status 200': (r) => r.status === 200 });
}

// ⚔️ 공격 2: 따닥 중복결제 폭파 로직
export function attackDoubleSubmit() {
    const reqs = [];
    const payload = JSON.stringify({
        userId: 9999, // 9999번 1명의 유저가
        productId: 888, // 888번 상품을 광클함
    });

    const params = { headers: { 'Content-Type': 'application/json' } };

    // 💡 K6의 비기: http.batch()
    // for문 돌려서 하나씩 쏘는 게 아니라, 5개의 요청을 장전했다가
    // 네트워크 단에서 0.0001초의 오차도 없이 완전 동시에 병렬(Concurrent) 발사해버림.
    for (let i = 0; i < 5; i++) {
        reqs.push(['POST', BASE_URL, payload, params]);
    }

    const responses = http.batch(reqs);

    // 5번의 요청이 전부 200 OK가 뜬다면 방어막이 아예 없다는 증거
    check(responses, {
        'Double Submit: all 5 requests success?!': (resps) => resps.every(r => r.status === 200)
    });
}