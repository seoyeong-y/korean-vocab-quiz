const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function request(path, options = {}) {
  const isFormData = options.body instanceof FormData;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
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

export function markQuizQuestionMastered(payload) {
  return request('/api/quizzes/mastered', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function unmarkQuizQuestionMastered(questionId) {
  return request(`/api/quizzes/mastered/${questionId}`, {
    method: 'DELETE',
  });
}

export function completeQuiz(payload) {
  return request('/api/statistics/quiz-completions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getStatisticsDashboard() {
  return request('/api/statistics/dashboard');
}

export function getWrongAnswers() {
  return request('/api/wrong-answers');
}

export function getMasteredVocabularies() {
  return request('/api/mastered-vocabularies');
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

export function extractVocabularyFromImages(files) {
  const formData = new FormData();
  Array.from(files).forEach((file) => {
    formData.append('files', file);
  });

  return request('/api/vocabularies/image/extract', {
    method: 'POST',
    headers: {},
    body: formData,
  });
}

export function saveVocabularyBatch(items) {
  return request('/api/vocabularies/batch', {
    method: 'POST',
    body: JSON.stringify({ items }),
  });
}
