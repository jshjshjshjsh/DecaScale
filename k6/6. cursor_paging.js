import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        // 💡 팩폭: 개선된 API는 가볍기 때문에 100명이 들이박아도 버팀
        cursor_paging_heaven: {
            executor: 'constant-vus',
            vus: 100,
            duration: '10s',
        },
    },
};

export default function () {
    // 동일하게 끝자락 위치를 요구하지만, 형의 id 수학 공식이 적용된 API 호출
    const res = http.get('http://localhost:58181/api/v2/orders?page=999990&size=10');
    check(res, { 'status is 200': (r) => r.status === 200 });
}