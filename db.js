const { Pool } = require('pg');
require('dotenv').config();

// DB 연결 설정 (전화기 설정)
const pool = new Pool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
});

// 연결이 잘 되었는지 확인용 테스트 쿼리
pool.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error('❌ DB 연결 에러:', err.message);
  } else {
    console.log('✅ PostgreSQL 연결 성공! 서버 시간:', res.rows[0].now);
  }
});

module.exports = pool;