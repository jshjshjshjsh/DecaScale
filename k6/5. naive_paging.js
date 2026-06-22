import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        // 💡 팩폭: 단 10명만 조회해도 DB CPU가 100% 치면서 터지는 걸 볼 수 있음
        deep_paging_hell: {
            executor: 'constant-vus',
            vus: 10,
            duration: '10s',
        },
    },
};

export default function () {
    // 1,000만 건의 거의 맨 끝자락인 999만 9900번째 데이터를 조회해 달라고 떼씀
    const res = http.get('http://localhost:58181/api/v1/orders/naive?page=999990&size=10');
    check(res, { 'status is 200': (r) => r.status === 200 });
}