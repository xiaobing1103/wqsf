package com.wqst.api.categorymatch;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.BizLicenseOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.BizLicenseOCRResponse;
import com.wqst.api.common.BusinessException;
import com.wqst.api.config.AppProperties;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BusinessLicenseOcrClient {
    private final AppProperties.Ocr properties;

    public BusinessLicenseOcrClient(AppProperties properties) {
        this.properties = properties.ocr();
    }

    public boolean configured() {
        return properties != null
                && StringUtils.hasText(properties.secretId())
                && StringUtils.hasText(properties.secretKey());
    }

    public OcrResult recognize(byte[] image) {
        if (!configured()) {
            throw new BusinessException("OCR_NOT_CONFIGURED", "本地尚未配置腾讯云 OCR，请手工填写识别内容后继续匹配", HttpStatus.SERVICE_UNAVAILABLE);
        }
        try {
            Credential credential = new Credential(properties.secretId(), properties.secretKey());
            HttpProfile http = new HttpProfile();
            http.setProtocol("https://");
            http.setEndpoint(StringUtils.hasText(properties.endpoint()) ? properties.endpoint() : "ocr.tencentcloudapi.com");
            http.setConnTimeout(10);
            http.setReadTimeout(30);
            http.setWriteTimeout(30);
            ClientProfile profile = new ClientProfile();
            profile.setHttpProfile(http);
            OcrClient client = new OcrClient(credential,
                    StringUtils.hasText(properties.region()) ? properties.region() : "ap-guangzhou", profile);
            BizLicenseOCRRequest request = new BizLicenseOCRRequest();
            request.setImageBase64(Base64.getEncoder().encodeToString(image));
            request.setEnableCopyWarn(true);
            request.setEnablePeriodComplete(true);
            BizLicenseOCRResponse response = client.BizLicenseOCR(request);
            if (!StringUtils.hasText(response.getName()) && !StringUtils.hasText(response.getBusiness())) {
                throw new BusinessException("OCR_NO_BIZ_LICENSE", "未识别到有效营业执照，请更换清晰图片或手工填写", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return new OcrResult(response.getName(), response.getBusiness(), response.getSetDate(), response.getRegNum(),
                    response.getCapital(), response.getRequestId());
        } catch (BusinessException ex) {
            throw ex;
        } catch (TencentCloudSDKException ex) {
            throw new BusinessException("OCR_SERVICE_UNAVAILABLE", "营业执照识别暂时不可用，请稍后重试或手工填写", HttpStatus.BAD_GATEWAY);
        }
    }

    public record OcrResult(String companyName, String businessScope, String establishedDate,
                            String creditCode, String registeredCapital, String providerRequestId) {}
}
