import React from 'react';
import { createRoot } from 'react-dom/client';
import { createQuiz, submitQuizAnswer } from './api';
import './styles.css';

const categories = [
  { value: 'NATIVE_KOREAN', label: '고유어' },
  { value: 'SINO_KOREAN', label: '한자어' },
  { value: 'LOANWORD', label: '외래어' },
  { value: 'PROVERB', label: '속담' },
  { value: 'IDIOM', label: '관용어' },
];

const quizModes = [
  { value: 'WORD_TO_MEANING', label: '단어 보고 뜻 맞히기' },
  { value: 'MEANING_TO_WORD', label: '뜻 보고 단어 맞히기' },
];

const initialSettings = {
  category: 'NATIVE_KOREAN',
  mode: 'WORD_TO_MEANING',
  questionCount: 5,
};

function App() {
  const [screen, setScreen] = React.useState('start');
  const [settings, setSettings] = React.useState(initialSettings);
  const [questions, setQuestions] = React.useState([]);
  const [currentIndex, setCurrentIndex] = React.useState(0);
  const [answers, setAnswers] = React.useState([]);
  const [selectedOptionId, setSelectedOptionId] = React.useState(null);
  const [feedback, setFeedback] = React.useState(null);
  const [loading, setLoading] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [error, setError] = React.useState('');

  const currentQuestion = questions[currentIndex];
  const progress = questions.length ? ((currentIndex + 1) / questions.length) * 100 : 0;
  const correctCount = answers.filter((answer) => answer.correct).length;
  const incorrectCount = answers.length - correctCount;
  const accuracy = answers.length ? Math.round((correctCount / answers.length) * 100) : 0;

  async function handleStart(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    setQuestions([]);
    setAnswers([]);
    setCurrentIndex(0);
    setSelectedOptionId(null);
    setFeedback(null);

    try {
      const quiz = await createQuiz({
        ...settings,
        questionCount: Number(settings.questionCount),
      });

      if (!Array.isArray(quiz) || quiz.length === 0) {
        setError('생성된 퀴즈 데이터가 없습니다. 카테고리와 문제 수를 다시 확인해 주세요.');
        setScreen('start');
        return;
      }

      setQuestions(quiz);
      setScreen('quiz');
    } catch (startError) {
      setError(normalizeError(startError.message));
      setScreen('start');
    } finally {
      setLoading(false);
    }
  }

  async function handleSelectOption(option) {
    if (!currentQuestion || feedback || submitting) {
      return;
    }

    setSelectedOptionId(option.optionId);
    setSubmitting(true);
    setError('');

    try {
      const result = await submitQuizAnswer({
        vocabularyId: currentQuestion.vocabularyId,
        mode: currentQuestion.mode,
        selectedOptionId: option.optionId,
      });

      const answer = {
        vocabularyId: currentQuestion.vocabularyId,
        selectedOptionId: option.optionId,
        selectedText: option.text,
        correct: result.correct,
        correctAnswer: result.correctAnswer,
      };

      setFeedback(answer);
      setAnswers((previousAnswers) => [...previousAnswers, answer]);
    } catch (submitError) {
      setSelectedOptionId(null);
      setError(normalizeError(submitError.message));
    } finally {
      setSubmitting(false);
    }
  }

  function handleNext() {
    if (currentIndex === questions.length - 1) {
      setScreen('result');
      return;
    }

    setCurrentIndex((index) => index + 1);
    setSelectedOptionId(null);
    setFeedback(null);
    setError('');
  }

  function handleRetry() {
    setScreen('start');
    setQuestions([]);
    setAnswers([]);
    setCurrentIndex(0);
    setSelectedOptionId(null);
    setFeedback(null);
    setError('');
  }

  return (
    <main className="app-shell">
      {screen === 'start' && (
        <StartScreen
          settings={settings}
          loading={loading}
          error={error}
          onChange={setSettings}
          onStart={handleStart}
        />
      )}

      {screen === 'quiz' && currentQuestion && (
        <QuizScreen
          question={currentQuestion}
          currentIndex={currentIndex}
          totalCount={questions.length}
          progress={progress}
          selectedOptionId={selectedOptionId}
          feedback={feedback}
          submitting={submitting}
          error={error}
          onSelectOption={handleSelectOption}
          onNext={handleNext}
        />
      )}

      {screen === 'result' && (
        <ResultScreen
          totalCount={questions.length}
          correctCount={correctCount}
          incorrectCount={incorrectCount}
          accuracy={accuracy}
          onRetry={handleRetry}
        />
      )}
    </main>
  );
}

function StartScreen({ settings, loading, error, onChange, onStart }) {
  function updateField(field, value) {
    onChange({
      ...settings,
      [field]: value,
    });
  }

  return (
    <section className="panel start-panel" aria-labelledby="start-title">
      <p className="eyebrow">KBS Korean Vocabulary Quiz</p>
      <h1 id="start-title">어휘 퀴즈</h1>
      <p className="description">
        카테고리와 방식을 고른 뒤 한 문제씩 풀어보세요.
      </p>

      <form className="quiz-form" onSubmit={onStart}>
        <fieldset>
          <legend>카테고리</legend>
          <div className="option-grid">
            {categories.map((category) => (
              <label className="choice" key={category.value}>
                <input
                  checked={settings.category === category.value}
                  name="category"
                  type="radio"
                  value={category.value}
                  onChange={(event) => updateField('category', event.target.value)}
                />
                <span>{category.label}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend>퀴즈 모드</legend>
          <div className="mode-group">
            {quizModes.map((mode) => (
              <label className="choice" key={mode.value}>
                <input
                  checked={settings.mode === mode.value}
                  name="mode"
                  type="radio"
                  value={mode.value}
                  onChange={(event) => updateField('mode', event.target.value)}
                />
                <span>{mode.label}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <label className="field-label" htmlFor="question-count">
          문제 수
          <input
            id="question-count"
            min="1"
            type="number"
            value={settings.questionCount}
            onChange={(event) => updateField('questionCount', event.target.value)}
          />
        </label>

        {error && <ErrorMessage message={error} />}

        <button className="primary-button" disabled={loading} type="submit">
          {loading ? '퀴즈 로딩 중' : '퀴즈 시작'}
        </button>
      </form>
    </section>
  );
}

function QuizScreen({
  question,
  currentIndex,
  totalCount,
  progress,
  selectedOptionId,
  feedback,
  submitting,
  error,
  onSelectOption,
  onNext,
}) {
  return (
    <section className="panel quiz-panel" aria-labelledby="quiz-title">
      <div className="quiz-header">
        <p className="question-count">
          {currentIndex + 1} / {totalCount}
        </p>
        <p className="mode-label">{modeLabel(question.mode)}</p>
      </div>

      <div className="progress-track" aria-label="진행률">
        <div className="progress-bar" style={{ width: `${progress}%` }} />
      </div>

      <h1 id="quiz-title" className="question-text">
        {question.questionText}
      </h1>

      <div className="options" role="list">
        {question.options.map((option) => (
          <button
            className={optionClassName(option, selectedOptionId, feedback)}
            disabled={Boolean(feedback) || submitting}
            key={option.optionId}
            type="button"
            onClick={() => onSelectOption(option)}
          >
            {option.text}
          </button>
        ))}
      </div>

      {submitting && <p className="status-message">정답 확인 중</p>}
      {error && <ErrorMessage message={error} />}
      {feedback && (
        <div className={feedback.correct ? 'feedback correct' : 'feedback incorrect'}>
          <strong>{feedback.correct ? '정답입니다.' : '오답입니다.'}</strong>
          {!feedback.correct && <span>정답: {feedback.correctAnswer}</span>}
        </div>
      )}

      <button
        className="primary-button next-button"
        disabled={!feedback}
        type="button"
        onClick={onNext}
      >
        {currentIndex === totalCount - 1 ? '결과 보기' : '다음 문제'}
      </button>
    </section>
  );
}

function ResultScreen({ totalCount, correctCount, incorrectCount, accuracy, onRetry }) {
  return (
    <section className="panel result-panel" aria-labelledby="result-title">
      <p className="eyebrow">Quiz Result</p>
      <h1 id="result-title">결과</h1>

      <dl className="result-grid">
        <div>
          <dt>총 문제 수</dt>
          <dd>{totalCount}</dd>
        </div>
        <div>
          <dt>정답 수</dt>
          <dd>{correctCount}</dd>
        </div>
        <div>
          <dt>오답 수</dt>
          <dd>{incorrectCount}</dd>
        </div>
        <div>
          <dt>정답률</dt>
          <dd>{accuracy}%</dd>
        </div>
      </dl>

      <button className="primary-button" type="button" onClick={onRetry}>
        다시 풀기
      </button>
    </section>
  );
}

function ErrorMessage({ message }) {
  return <p className="error-message">{message}</p>;
}

function modeLabel(mode) {
  return quizModes.find((item) => item.value === mode)?.label || mode;
}

function optionClassName(option, selectedOptionId, feedback) {
  const classNames = ['option-button'];

  if (selectedOptionId === option.optionId) {
    classNames.push('selected');
  }

  if (feedback && option.text === feedback.correctAnswer) {
    classNames.push('correct-option');
  }

  if (feedback && selectedOptionId === option.optionId && !feedback.correct) {
    classNames.push('wrong-option');
  }

  return classNames.join(' ');
}

function normalizeError(message) {
  if (message.includes('At least 4 vocabularies')) {
    return '해당 카테고리에 4지선다 문제를 만들 수 있는 어휘가 부족합니다.';
  }
  if (message.includes('questionCount')) {
    return '문제 수가 해당 카테고리의 전체 어휘 수보다 많습니다.';
  }
  return message;
}

createRoot(document.getElementById('root')).render(<App />);
