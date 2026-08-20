import React from 'react';
import { createRoot } from 'react-dom/client';
import {
  createQuiz,
  createWrongAnswerReviewQuiz,
  deleteAllWrongAnswers,
  deleteWrongAnswer,
  getWrongAnswers,
  submitQuizAnswer,
} from './api';
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
  const [reviewSettings, setReviewSettings] = React.useState({
    mode: 'WORD_TO_MEANING',
    questionCount: 1,
  });
  const [quizType, setQuizType] = React.useState('general');
  const [questions, setQuestions] = React.useState([]);
  const [currentIndex, setCurrentIndex] = React.useState(0);
  const [answers, setAnswers] = React.useState([]);
  const [selectedOptionId, setSelectedOptionId] = React.useState(null);
  const [feedback, setFeedback] = React.useState(null);
  const [wrongAnswers, setWrongAnswers] = React.useState([]);
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
    setQuizType('general');

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

  async function loadWrongAnswers() {
    setLoading(true);
    setError('');

    try {
      const items = await getWrongAnswers();
      setWrongAnswers(Array.isArray(items) ? items : []);
      setScreen('wrongAnswers');
    } catch (listError) {
      setError(normalizeError(listError.message));
      setScreen('wrongAnswers');
    } finally {
      setLoading(false);
    }
  }

  async function handleStartReview(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    setQuestions([]);
    setAnswers([]);
    setCurrentIndex(0);
    setSelectedOptionId(null);
    setFeedback(null);
    setQuizType('review');

    try {
      const quiz = await createWrongAnswerReviewQuiz({
        mode: reviewSettings.mode,
        questionCount: Number(reviewSettings.questionCount),
      });

      if (!Array.isArray(quiz) || quiz.length === 0) {
        setError('복습할 오답 퀴즈 데이터가 없습니다.');
        setScreen('wrongAnswers');
        return;
      }

      setQuestions(quiz);
      setScreen('quiz');
    } catch (reviewError) {
      setError(normalizeError(reviewError.message));
      setScreen('wrongAnswers');
    } finally {
      setLoading(false);
    }
  }

  async function handleDeleteWrongAnswer(id) {
    setLoading(true);
    setError('');

    try {
      await deleteWrongAnswer(id);
      const items = await getWrongAnswers();
      setWrongAnswers(Array.isArray(items) ? items : []);
    } catch (deleteError) {
      setError(normalizeError(deleteError.message));
    } finally {
      setLoading(false);
    }
  }

  async function handleDeleteAllWrongAnswers() {
    setLoading(true);
    setError('');

    try {
      await deleteAllWrongAnswers();
      setWrongAnswers([]);
    } catch (deleteError) {
      setError(normalizeError(deleteError.message));
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
        questionId: currentQuestion.questionId,
        selectedOptionId: option.optionId,
        wrongAnswerReview: quizType === 'review',
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
    setQuizType('general');
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
          onOpenWrongAnswers={loadWrongAnswers}
        />
      )}

      {screen === 'wrongAnswers' && (
        <WrongAnswerScreen
          items={wrongAnswers}
          settings={reviewSettings}
          loading={loading}
          error={error}
          onChange={setReviewSettings}
          onBack={handleRetry}
          onRefresh={loadWrongAnswers}
          onDelete={handleDeleteWrongAnswer}
          onDeleteAll={handleDeleteAllWrongAnswers}
          onStartReview={handleStartReview}
        />
      )}

      {screen === 'quiz' && currentQuestion && (
        <QuizScreen
          question={currentQuestion}
          quizType={quizType}
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
          quizType={quizType}
          onRetry={handleRetry}
          onWrongAnswers={loadWrongAnswers}
        />
      )}
    </main>
  );
}

function StartScreen({ settings, loading, error, onChange, onStart, onOpenWrongAnswers }) {
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

      <div className="secondary-actions">
        <button className="secondary-button" disabled={loading} type="button" onClick={onOpenWrongAnswers}>
          오답 목록 보기
        </button>
      </div>
    </section>
  );
}

function WrongAnswerScreen({
  items,
  settings,
  loading,
  error,
  onChange,
  onBack,
  onRefresh,
  onDelete,
  onDeleteAll,
  onStartReview,
}) {
  function updateField(field, value) {
    onChange({
      ...settings,
      [field]: value,
    });
  }

  return (
    <section className="panel wrong-answer-panel" aria-labelledby="wrong-answer-title">
      <div className="screen-heading">
        <div>
          <p className="eyebrow">Wrong Answer Review</p>
          <h1 id="wrong-answer-title">오답 목록</h1>
        </div>
        <button className="secondary-button" disabled={loading} type="button" onClick={onBack}>
          돌아가기
        </button>
      </div>

      {error && <ErrorMessage message={error} />}

      <div className="toolbar">
        <button className="secondary-button" disabled={loading} type="button" onClick={onRefresh}>
          새로고침
        </button>
        <button
          className="danger-button"
          disabled={loading || items.length === 0}
          type="button"
          onClick={onDeleteAll}
        >
          전체 삭제
        </button>
      </div>

      {loading && <p className="status-message">오답 목록을 불러오는 중</p>}

      {!loading && items.length === 0 && (
        <div className="empty-state">
          <strong>저장된 오답이 없습니다.</strong>
          <span>일반 퀴즈에서 틀린 문제가 생기면 이곳에 자동으로 저장됩니다.</span>
        </div>
      )}

      {items.length > 0 && (
        <>
          <form className="review-form" onSubmit={onStartReview}>
            <fieldset>
              <legend>복습 모드</legend>
              <div className="mode-group">
                {quizModes.map((mode) => (
                  <label className="choice" key={mode.value}>
                    <input
                      checked={settings.mode === mode.value}
                      name="review-mode"
                      type="radio"
                      value={mode.value}
                      onChange={(event) => updateField('mode', event.target.value)}
                    />
                    <span>{mode.label}</span>
                  </label>
                ))}
              </div>
            </fieldset>

            <label className="field-label" htmlFor="review-question-count">
              복습 문제 수
              <input
                id="review-question-count"
                min="1"
                type="number"
                value={settings.questionCount}
                onChange={(event) => updateField('questionCount', event.target.value)}
              />
            </label>

            <button className="primary-button" disabled={loading} type="submit">
              {loading ? '복습 퀴즈 로딩 중' : '오답 복습 시작'}
            </button>
          </form>

          <div className="wrong-answer-list" role="list">
            {items.map((item) => (
              <article className="wrong-answer-item" key={item.id} role="listitem">
                <div>
                  <h2>{item.word}</h2>
                  <p>{item.meaning}</p>
                  <dl className="meta-list">
                    <div>
                      <dt>카테고리</dt>
                      <dd>{categoryLabel(item.category)}</dd>
                    </div>
                    <div>
                      <dt>퀴즈 모드</dt>
                      <dd>{modeLabel(item.quizMode)}</dd>
                    </div>
                    <div>
                      <dt>틀린 횟수</dt>
                      <dd>{item.wrongCount}</dd>
                    </div>
                    <div>
                      <dt>마지막 오답</dt>
                      <dd>{formatDateTime(item.lastWrongAt)}</dd>
                    </div>
                  </dl>
                </div>
                <button className="danger-button" disabled={loading} type="button" onClick={() => onDelete(item.id)}>
                  삭제
                </button>
              </article>
            ))}
          </div>
        </>
      )}
    </section>
  );
}

function QuizScreen({
  question,
  quizType,
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
        <p className="mode-label">
          {quizType === 'review' ? '오답 복습' : '일반 퀴즈'} · {modeLabel(question.mode)}
        </p>
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

function ResultScreen({ totalCount, correctCount, incorrectCount, accuracy, quizType, onRetry, onWrongAnswers }) {
  return (
    <section className="panel result-panel" aria-labelledby="result-title">
      <p className="eyebrow">{quizType === 'review' ? 'Review Result' : 'Quiz Result'}</p>
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
      {quizType === 'review' && (
        <button className="secondary-button result-secondary-button" type="button" onClick={onWrongAnswers}>
          오답 목록으로
        </button>
      )}
    </section>
  );
}

function ErrorMessage({ message }) {
  return <p className="error-message">{message}</p>;
}

function modeLabel(mode) {
  return quizModes.find((item) => item.value === mode)?.label || mode;
}

function categoryLabel(category) {
  return categories.find((item) => item.value === category)?.label || category;
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
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
    return '4지선다 문제를 만들 수 있는 어휘가 부족합니다.';
  }
  if (message.includes('No wrong answers')) {
    return '복습할 오답이 없습니다.';
  }
  if (message.includes('Question is not valid or has expired')) {
    return '문제 정보가 만료되었습니다. 퀴즈를 다시 시작해 주세요.';
  }
  if (message.includes('Selected option is not included')) {
    return '현재 문제의 선택지가 아닙니다. 퀴즈를 다시 시작해 주세요.';
  }
  if (message.includes('questionCount')) {
    return '문제 수가 출제 가능한 어휘 수보다 많습니다.';
  }
  return message;
}

createRoot(document.getElementById('root')).render(<App />);
