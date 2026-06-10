package ru.practicum.analyzer.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.model.RecommendationDto;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.stats.proto.*;

import java.util.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerImpl extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<RecommendationDto> recommendations = recommendationService.getRecommendationsForUser(request.getUserId(), request.getMaxResults());

            recommendations.stream()
                    .map(this::toProto)
                    .forEach(responseObserver::onNext);

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций для пользователя {}", request.getUserId(), e);

            responseObserver.onError(Status.INTERNAL.withDescription("Не удалось получить рекомендации").asException());
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<RecommendationDto> recommendations = recommendationService.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults());

            recommendations.stream()
                    .map(this::toProto)
                    .forEach(responseObserver::onNext);

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка при поиске похожих событий {}", request.getEventId(), e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Не удалось получить похожие события")
                            .asException()
            );
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<RecommendationDto> interactions =
                    recommendationService.getInteractionsCount(
                            request.getEventIdList()
                    );

            interactions.stream()
                    .map(this::toProto)
                    .forEach(responseObserver::onNext);

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка при получении статистики взаимодействий", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Не удалось получить статистику")
                            .asException()
            );
        }
    }

    private RecommendedEventProto toProto(RecommendationDto dto) {
        return RecommendedEventProto.newBuilder()
                .setEventId(dto.getEventId())
                .setScore(dto.getScore())
                .build();
    }
}