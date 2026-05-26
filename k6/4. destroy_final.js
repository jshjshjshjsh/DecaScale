import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        // 💥 200명이 10초 동안 쉬지 않고 777번(재고 10개)을 무지성 연타함!
        redis_defense_test: {
            executor: 'constant-vus',
            vus: 200,
            duration: '10s',
            exec: 'attackStockWithRedis',
        },
    },
};

const BASE_URL = 'http://localhost:58181/api/redis/orders';

export function attackStockWithRedis() {
    const payload = JSON.stringify({
        userId: Math.floor(Math.random() * 100000) + 1,
        productId: 777, // 네스프레소 오픈런 상품
        // 유니크 인덱스에 막히지 않고 안쪽 로직(Redis)까지 도달하도록 무작위 ID 부여
        requestId: 'redis-req-' + Math.random().toString(36).substring(2) + '-' + Date.now()
    });

    const params = { headers: { 'Content-Type': 'application/json' } };
    const res = http.post(BASE_URL, payload, params);

    // 💡 팩폭: 이번엔 타임아웃(30초) 없이, 아주 빠르고 깔끔하게
    // 성공(200) 딱 10개, 나머지 수만 개는 입구컷 예외(500)가 떨어져야 정상!
    check(res, {
        'Handled fast (200 or 400)': (r) => r.status === 200 || r.status === 400
    });
}