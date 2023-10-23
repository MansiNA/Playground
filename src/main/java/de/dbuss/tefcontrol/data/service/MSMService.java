package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.ProductHierarchie;
import de.dbuss.tefcontrol.data.repository.ProductHierarchieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Service
public class MSMService {
    @Autowired
    private JdbcTemplate template;

    private final ProductHierarchieRepository productHierarchieRepository;

    private final ProjectConnectionService projectConnectionService;

    public MSMService(ProductHierarchieRepository productHierarchieRepository, ProjectConnectionService projectConnectionService) {
        this.productHierarchieRepository = productHierarchieRepository;
        this.projectConnectionService = projectConnectionService;
    }

    public List<ProductHierarchie> findAllProducts(String stringFilter) {
        if (stringFilter == null || stringFilter.isEmpty()) {
            return productHierarchieRepository.findAll();
        } else {
            return productHierarchieRepository.search(stringFilter);
        }
    }

    public long countProducts() {
        return productHierarchieRepository.count();
    }

    public void deleteProduct(ProductHierarchie product) {
        productHierarchieRepository.delete(product);
    }

    public void saveProduct(ProductHierarchie product) {
        if (product == null) {

            System.err.println("Product is null. Are you sure you have connected your form to the application?");
            return;
        }

        if (product.getId() == null) {
            product.setId(countProducts() + 1);
        }

        productHierarchieRepository.save(product);
    }

    public String startJob(String jobname, String agentDb){

        String hostName = projectConnectionService.findByName(agentDb).get().getHostname();
        DataSource dataSource = projectConnectionService.getDataSource(agentDb);
        template = new JdbcTemplate(dataSource);

        try {
            String sql = "msdb.dbo.sp_start_job @job_name='" + jobname + "'";
            template.execute(sql);
        }
        catch (CannotGetJdbcConnectionException connectionException) {
            return "Fehler beim Herstellen der TCP/IP-Verbindung mit dem Host '"+ hostName +"', Port 1433. Fehler: '"+ hostName +"'. Überprüfen Sie die Verbindungseigenschaften. Stellen Sie sicher, dass eine SQL Server-Instanz auf dem Host ausgeführt wird und am Port TCP/IP-Verbindungen akzeptiert";
        } catch (Exception e) {
            // Handle other exceptions
            return e.getMessage();
        }
        return "OK";

    }

}
