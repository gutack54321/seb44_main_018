package com.saecdo18.petmily;

import java.io.IOException;

public class WebpTest {
    public static void main(String[] args) {
        String inputImagePath = "input.png"; // 입력 PNG 파일 경로
        String outputImagePath = "output.webp"; // 출력 WebP 파일 경로

        try {
            // 실행할 cwebp 명령을 생성
            ProcessBuilder processBuilder = new ProcessBuilder("cwebp", inputImagePath, "-o", outputImagePath);

            // 명령 실행
            Process process = processBuilder.start();

            // 명령 실행이 완료될 때까지 대기
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("PNG 파일이 WebP로 변환되었습니다.");
            } else {
                System.err.println("PNG 파일을 WebP로 변환하는 중 오류가 발생했습니다.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
