package com.saecdo18.petmily.awsConfig;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class S3UploadService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String saveFile(MultipartFile multipartFile, String originalFilename) throws IOException {



        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        amazonS3.putObject(bucket, originalFilename, multipartFile.getInputStream(), metadata);
//        amazonS3.putObject(bucket, originalFilename, webpFile);
        return amazonS3.getUrl(bucket, originalFilename).toString();
    }

    public void deleteImage(String originalFilename) {
        amazonS3.deleteObject(bucket, originalFilename);
    }

    private void convertToWebP(File input, File output) throws IOException {
        log.info("input: {}", input.getName());
        BufferedImage image = ImageIO.read(input);
        ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        try (FileImageOutputStream out = new FileImageOutputStream(output)) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
