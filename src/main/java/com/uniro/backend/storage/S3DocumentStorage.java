package com.uniro.backend.storage;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3DocumentStorage {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public String upload(
            Long userId,
            MultipartFile file,
            String contentType
    ) throws IOException {

        String extension = contentType.equals("image/png")
                ? ".png"
                : ".jpg";

        String key = "enrollment/"
                + userId
                + "/"
                + UUID.randomUUID()
                + extension;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(file.getBytes())
            );

            System.out.println("S3 파일 업로드 성공: " + key);

        } catch (Exception e) {

            System.err.println("========== S3 업로드 오류 ==========");
            System.err.println("Bucket: " + bucket);
            System.err.println("Key: " + key);
            System.err.println("오류 메시지: " + e.getMessage());

            e.printStackTrace();

            throw e;
        }

        // 공개 HTTP URL이 아닌 내부 객체 위치
        return "s3://" + bucket + "/" + key;
    }

    public void delete(String documentUrl) {

        if (documentUrl == null || documentUrl.isBlank()) {
            return;
        }

        String prefix = "s3://" + bucket + "/";

        if (!documentUrl.startsWith(prefix)) {
            throw new IllegalArgumentException(
                    "현재 버킷에 속하지 않는 S3 객체입니다."
            );
        }

        String key = documentUrl.substring(prefix.length());

        if (!key.startsWith("enrollment/")) {
            throw new IllegalArgumentException(
                    "인증 문서 경로가 아닙니다."
            );
        }

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
    }
}