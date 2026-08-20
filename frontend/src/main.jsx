import React from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

function App() {
  const [backendStatus, setBackendStatus] = React.useState('checking');

  React.useEffect(() => {
    fetch(`${apiBaseUrl}/actuator/health`)
      .then((response) => {
        if (!response.ok) {
          throw new Error('Backend health check failed');
        }
        return response.json();
      })
      .then((data) => setBackendStatus(data.status || 'unknown'))
      .catch(() => setBackendStatus('unavailable'));
  }, []);

  return (
    <main className="app-shell">
      <section className="intro">
        <p className="eyebrow">KBS Korean Vocabulary Quiz</p>
        <h1>KBS한국어능력시험 어휘 학습</h1>
        <p className="description">
          프로젝트 초기 실행 환경이 준비되었습니다. 어휘 CRUD, CSV 등록, 퀴즈 기능은 이후 PR에서 구현합니다.
        </p>
        <dl className="status-grid">
          <div>
            <dt>Frontend</dt>
            <dd>Running</dd>
          </div>
          <div>
            <dt>Backend</dt>
            <dd>{backendStatus}</dd>
          </div>
        </dl>
      </section>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);

