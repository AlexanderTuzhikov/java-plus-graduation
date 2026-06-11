package ru.practicum.analyzer.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerImpl extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {

            log.info("Получен запрос рекомендаций: userId={}, maxResults={}",
                    request.getUserId(), request.getMaxResults());

            sendResponse(recommendationService.getRecommendations(request.getUserId(), request.getMaxResults()), responseObserver);
        } catch (Exception e) {
            handleError(responseObserver, "Ошибка получения рекомендаций", e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {

            log.info("Получен запрос похожих событий: eventId={}, userId={}, maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults()
            );

            sendResponse(recommendationService.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults()), responseObserver);
        } catch (Exception e) {
            handleError(responseObserver, "Ошибка получения похожих событий", e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {

            log.info("Получен запрос количества взаимодействий для {} событий", request.getEventIdList().size());

            sendResponse(recommendationService.getInteractionsCount(request.getEventIdList()), responseObserver);

        } catch (Exception e) {
            handleError(responseObserver, "Ошибка получения количества взаимодействий", e);
        }
    }

    private void sendResponse(List<RecommendedEventProto> response, StreamObserver<RecommendedEventProto> observer) {
        response.forEach(observer::onNext);
        observer.onCompleted();
    }

    private void handleError(StreamObserver<?> observer, String message, Exception exception) {
        log.error(message, exception);

        observer.onError(Status.INTERNAL.withDescription(message).asRuntimeException());
    }
}