import org.springframework.web.bind.annotation.GetMapping;

public interface DefaultPackageApi {

    @GetMapping("/v1/default-package")
    String defaultPackage();
}
