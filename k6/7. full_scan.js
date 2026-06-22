import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        // 💡 팩폭: 인덱스 없이 1000만 건을 뒤지는 게 얼마나 끔찍한지 볼 거니까,
        // 서버 보호를 위해 VUs는 딱 5명으로 제한할게!
        no_index_hell: {
            executor: 'constant-vus',
            vus: 5,
            duration: '10s',
        },
    },
};

export default function () {
    // 1 ~ 100,000 사이의 무작위 유저 ID로 마이페이지 조회를 요청함
    const randomUserId = Math.floor(Math.random() * 100000) + 1;
    const res = http.get(`http://localhost:58181/api/v1/orders/user/${randomUserId}`);

    check(res, { 'status is 200': (r) => r.status === 200 });
}