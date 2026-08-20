const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data?.messages?.length
      ? data.messages.join('\n')
      : '요청을 처리하지 못했습니다.';
    throw new Error(message);
  }

  return data;
}

export function createQuiz(payload) {
  return request('/api/quizzes', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function submitQuizAnswer(payload) {
  return request('/api/quizzes/submit', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getWrongAnswers() {
  return request('/api/wrong-answers');
}

export function createWrongAnswerReviewQuiz(payload) {
  return request('/api/wrong-answers/quizzes', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function deleteWrongAnswer(id) {
  return request(`/api/wrong-answers/${id}`, {
    method: 'DELETE',
  });
}

export function deleteAllWrongAnswers() {
  return request('/api/wrong-answers', {
    method: 'DELETE',
  });
}
