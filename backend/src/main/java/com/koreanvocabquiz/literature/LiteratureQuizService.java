package com.koreanvocabquiz.literature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LiteratureQuizService {
    private static final int OPTION_COUNT = 4;

    private final LiteraryAuthorRepository authorRepository;
    private final LiteraryWorkRepository workRepository;
    private final LiteraryFeatureRepository featureRepository;
    private final LiteraryQuizQuestionSessionStore sessionStore;

    public LiteratureQuizService(
            LiteraryAuthorRepository authorRepository,
            LiteraryWorkRepository workRepository,
            LiteraryFeatureRepository featureRepository,
            LiteraryQuizQuestionSessionStore sessionStore
    ) {
        this.authorRepository = authorRepository;
        this.workRepository = workRepository;
        this.featureRepository = featureRepository;
        this.sessionStore = sessionStore;
    }

    public List<LiteraryQuizAvailabilityResponse> availability() {
        List<LiteraryQuizCandidate> candidates = candidates();
        return List.of(
                new LiteraryQuizAvailabilityResponse(LiteratureQuizType.WORK_GUESS, candidates.stream().filter(candidate -> candidate.quizType() == LiteratureQuizType.WORK_GUESS).count()),
                new LiteraryQuizAvailabilityResponse(LiteratureQuizType.AUTHOR_GUESS, candidates.stream().filter(candidate -> candidate.quizType() == LiteratureQuizType.AUTHOR_GUESS).count())
        );
    }

    public List<LiteraryQuizQuestionResponse> create(LiteraryQuizCreateRequest request) {
        List<LiteraryQuizCandidate> available = candidates().stream()
                .filter(candidate -> candidate.quizType() == request.quizType())
                .collect(Collectors.toCollection(ArrayList::new));
        if (request.questionCount() > available.size()) {
            throw new LiteratureValidationException("문학 퀴즈 출제 가능 문제가 부족합니다. 최대 출제 가능 문제 수: " + available.size());
        }
        Collections.shuffle(available);
        return available.stream().limit(request.questionCount()).map(this::materialize).toList();
    }

    public LiteraryQuizSubmitResponse submit(String questionId, LiteraryQuizSubmitRequest request) {
        LiteraryQuizQuestionSession session = sessionStore.findValid(questionId)
                .orElseThrow(() -> new LiteratureValidationException("문학 퀴즈 문제가 만료되었거나 존재하지 않습니다."));
        if (!session.optionAnswers().containsKey(request.selectedOptionId())) {
            throw new LiteratureValidationException("선택한 보기가 해당 문제에 포함되어 있지 않습니다.");
        }
        return new LiteraryQuizSubmitResponse(session.correctOptionId().equals(request.selectedOptionId()), session.correctAnswer());
    }

    private List<LiteraryQuizCandidate> candidates() {
        List<LiteraryWork> works = workRepository.findAllByOrderByTitleAsc();
        List<LiteraryAuthor> authors = authorRepository.findAllByOrderByNameAsc();
        Map<Long, List<LiteraryWork>> worksByAuthor = works.stream().collect(Collectors.groupingBy(work -> work.getAuthor().getId()));
        List<LiteraryQuizCandidate> candidates = new ArrayList<>();
        for (LiteraryFeature feature : featureRepository.findAllByOrderByIdAsc()) {
            if (feature.getType() == LiteratureFeatureType.WORK && feature.getWork() != null) {
                addWorkCandidates(candidates, feature, works, authors);
            } else if (feature.getType() == LiteratureFeatureType.AUTHOR) {
                addAuthorCandidates(candidates, feature, worksByAuthor);
            }
        }
        return candidates;
    }

    private void addWorkCandidates(List<LiteraryQuizCandidate> candidates, LiteraryFeature feature, List<LiteraryWork> works, List<LiteraryAuthor> authors) {
        LiteraryWork correctWork = feature.getWork();
        List<LiteraryWork> workOptions = distinctWorks(works.stream().filter(work -> !work.getAuthor().getId().equals(correctWork.getAuthor().getId()) && !work.getTitle().equals(correctWork.getTitle())).toList());
        if (workOptions.size() >= OPTION_COUNT - 1) candidates.add(new LiteraryQuizCandidate(LiteratureQuizType.WORK_GUESS, feature, correctWork, workOptions.subList(0, OPTION_COUNT - 1), List.of(), List.of(correctWork.getTitle())));
        List<LiteraryAuthor> authorOptions = distinctAuthors(authors.stream().filter(author -> !author.getId().equals(correctWork.getAuthor().getId())).toList());
        if (authorOptions.size() >= OPTION_COUNT - 1) candidates.add(new LiteraryQuizCandidate(LiteratureQuizType.AUTHOR_GUESS, feature, correctWork, List.of(), authorOptions.subList(0, OPTION_COUNT - 1), List.of(correctWork.getTitle())));
    }

    private void addAuthorCandidates(List<LiteraryQuizCandidate> candidates, LiteraryFeature feature, Map<Long, List<LiteraryWork>> worksByAuthor) {
        LiteraryAuthor author = feature.getAuthor();
        List<LiteraryWork> authorWorks = new ArrayList<>(worksByAuthor.getOrDefault(author.getId(), List.of()));
        if (authorWorks.isEmpty()) return;
        List<LiteraryWork> allWorks = workRepository.findAllByOrderByTitleAsc();
        List<LiteraryWork> workOptions = distinctWorks(allWorks.stream().filter(work -> !work.getAuthor().getId().equals(author.getId())).toList());
        if (workOptions.size() >= OPTION_COUNT - 1) {
            for (LiteraryWork correctWork : authorWorks) candidates.add(new LiteraryQuizCandidate(LiteratureQuizType.WORK_GUESS, feature, correctWork, workOptions.subList(0, OPTION_COUNT - 1), List.of(), List.of()));
        }
        List<LiteraryAuthor> authors = distinctAuthors(allWorks.stream().map(LiteraryWork::getAuthor).filter(candidate -> !candidate.getId().equals(author.getId())).toList());
        if (authors.size() >= OPTION_COUNT - 1) candidates.add(new LiteraryQuizCandidate(LiteratureQuizType.AUTHOR_GUESS, feature, null, List.of(), authors.subList(0, OPTION_COUNT - 1), authorWorks.stream().limit(4).map(LiteraryWork::getTitle).toList()));
    }

    private LiteraryQuizQuestionResponse materialize(LiteraryQuizCandidate candidate) {
        List<String> answers = new ArrayList<>();
        String correctAnswer;
        String questionText;
        if (candidate.quizType() == LiteratureQuizType.WORK_GUESS) {
            correctAnswer = candidate.correctWork().getTitle();
            answers.add(correctAnswer);
            answers.addAll(candidate.workOptions().stream().map(LiteraryWork::getTitle).toList());
            questionText = candidate.feature().getType() == LiteratureFeatureType.AUTHOR ? "다음 중 이 작가의 작품은?" : "다음 중 해당 작품은?";
        } else {
            correctAnswer = candidate.feature().getAuthor().getName();
            answers.add(correctAnswer);
            answers.addAll(candidate.authorOptions().stream().map(LiteraryAuthor::getName).toList());
            questionText = candidate.feature().getType() == LiteratureFeatureType.AUTHOR
                    ? "이 작가는?"
                    : "이 작품의 작가는?";
        }
        if (answers.stream().distinct().count() != OPTION_COUNT) throw new LiteratureValidationException("정답이 하나인 4지선다를 만들 수 없습니다.");
        List<LiteraryQuizOptionResponse> options = new ArrayList<>();
        Map<String, String> optionAnswers = new LinkedHashMap<>();
        String correctOptionId = null;
        Collections.shuffle(answers);
        for (String answer : answers) {
            String optionId = sessionStore.nextId();
            options.add(new LiteraryQuizOptionResponse(optionId, answer));
            optionAnswers.put(optionId, answer);
            if (answer.equals(correctAnswer)) correctOptionId = optionId;
        }
        String questionId = sessionStore.nextId();
        sessionStore.save(new LiteraryQuizQuestionSession(questionId, candidate.quizType(), Map.copyOf(optionAnswers), correctOptionId, correctAnswer, sessionStore.expiresAt()));
        return new LiteraryQuizQuestionResponse(questionId, candidate.quizType(), candidate.feature().getAuthor().getName(), candidate.workTitles(), candidate.feature().getContent(), questionText, options);
    }

    private List<LiteraryWork> distinctWorks(List<LiteraryWork> works) { return new ArrayList<>(works.stream().collect(Collectors.toMap(LiteraryWork::getTitle, Function.identity(), (a, b) -> a, LinkedHashMap::new)).values()); }
    private List<LiteraryAuthor> distinctAuthors(List<LiteraryAuthor> authors) { return new ArrayList<>(authors.stream().collect(Collectors.toMap(LiteraryAuthor::getName, Function.identity(), (a, b) -> a, LinkedHashMap::new)).values()); }

    private record LiteraryQuizCandidate(LiteratureQuizType quizType, LiteraryFeature feature, LiteraryWork correctWork, List<LiteraryWork> workOptions, List<LiteraryAuthor> authorOptions, List<String> workTitles) {}
}
