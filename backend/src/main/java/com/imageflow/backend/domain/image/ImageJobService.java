package com.imageflow.backend.domain.image;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imageflow.backend.common.exception.BadRequestException;
import com.imageflow.backend.common.exception.NotFoundException;
import com.imageflow.backend.domain.image.dto.CreateImageJobRequest;
import com.imageflow.backend.domain.image.dto.ImageJobResponse;
import com.imageflow.backend.domain.usage.UsageRecord;
import com.imageflow.backend.domain.usage.UsageRecordRepository;
import com.imageflow.backend.domain.usage.UsageType;
import com.imageflow.backend.domain.user.User;
import com.imageflow.backend.domain.user.UserRepository;

@Service
@Transactional
public class ImageJobService {

    private final UserRepository userRepository;
    private final ImageJobRepository imageJobRepository;
    private final UsageRecordRepository usageRecordRepository;

    public ImageJobService(
            UserRepository userRepository,
            ImageJobRepository imageJobRepository,
            UsageRecordRepository usageRecordRepository
    ) {
        this.userRepository = userRepository;
        this.imageJobRepository = imageJobRepository;
        this.usageRecordRepository = usageRecordRepository;
    }

    public ImageJobResponse create(CreateImageJobRequest request) {
        if (request.userId() == null) {
            throw new BadRequestException("userId is required");
        }
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw new BadRequestException("prompt is required");
        }

        int creditsToUse = request.creditsToUse() == null ? 1 : request.creditsToUse();
        if (creditsToUse <= 0) {
            throw new BadRequestException("creditsToUse must be greater than zero");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("user not found: " + request.userId()));

        user.chargeCredits(creditsToUse);

        ImageJob imageJob = new ImageJob(user, request.prompt().trim(), creditsToUse);
        if (request.sourceImageUrl() != null && !request.sourceImageUrl().isBlank()) {
            imageJob.setSourceImageUrl(request.sourceImageUrl().trim());
        }
        user.addImageJob(imageJob);
        ImageJob savedJob = imageJobRepository.save(imageJob);

        UsageRecord usageRecord = new UsageRecord(
                user,
                UsageType.IMAGE_GENERATION,
                creditsToUse,
                savedJob.getId().toString(),
                "Credits used for image job"
        );
        user.addUsageRecord(usageRecord);
        usageRecordRepository.save(usageRecord);

        return ImageJobResponse.from(savedJob);
    }

    @Transactional(readOnly = true)
    public ImageJobResponse get(UUID imageJobId) {
        ImageJob imageJob = imageJobRepository.findById(imageJobId)
                .orElseThrow(() -> new NotFoundException("image job not found: " + imageJobId));
        return ImageJobResponse.from(imageJob);
    }
}
