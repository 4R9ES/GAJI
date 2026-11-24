package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

@Component
public class PostImageStorage implements WebMvcConfigurer {
    private final Path directory;

    public PostImageStorage(@Value("${gaji.upload-directory:uploads}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    Path save(MultipartFile image) {
        String extension = imageExtension(image);
        Path destination = null;
        try {
            Files.createDirectories(directory);
            destination = Files.createTempFile(directory, "post-", extension);
            try (var input = image.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return destination;
        } catch (IOException e) {
            deleteAfterFailure(destination, e);
            throw new UncheckedIOException("게시글 사진을 저장하지 못했습니다.", e);
        }
    }

    private String imageExtension(MultipartFile image) {
        try (var bytes = image.getInputStream();
             var input = new MemoryCacheImageInputStream(bytes)) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
            }
            var reader = readers.next();
            try {
                String extension = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg" -> ".jpg";
                    case "png" -> ".png";
                    default -> throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
                };
                reader.setInput(input);
                // 형식뿐 아니라 이미지 헤더도 읽을 수 있는지 확인한다.
                reader.getWidth(0);
                reader.getHeight(0);
                return extension;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
    }

    void deleteAfterFailure(Path image, Throwable failure) {
        if (image != null) {
            try {
                Files.deleteIfExists(image);
            } catch (IOException e) {
                failure.addSuppressed(e);
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = directory.toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
