import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefaultPackageController implements DefaultPackageApi {

    @Override
    public String defaultPackage() {
        return "";
    }
}
