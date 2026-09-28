package com.wqst.api.auth;

import com.wqst.api.account.entity.UserAccountEntity;
import com.wqst.api.account.mapper.UserAccountMapper;
import com.wqst.api.company.CompanyService;
import com.wqst.api.config.AppProperties;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@Order(100)
public class LocalDevDataRunner implements ApplicationRunner {
    private final UserAccountMapper users;private final CompanyService companies;private final AppProperties props;
    public LocalDevDataRunner(UserAccountMapper users,CompanyService companies,AppProperties props){this.users=users;this.companies=companies;this.props=props;}
    @Override public void run(ApplicationArguments args){if(props.auth()==null||!props.auth().devClientLoginEnabled()||users.findByUsername("local_client")!=null)return;UserAccountEntity client=new UserAccountEntity();client.setAccountType("CLIENT");client.setUsername("local_client");client.setDisplayName("本地测试客户");client.setStatus("ENABLED");users.insert(client);users.assignRole(client.getId(),"CLIENT");CompanyService.CompanyView company=companies.create(new CompanyService.CreateCompany("示例科技有限公司","91430100DEMO000001","测试联系人","13800000000","示例法人","湖南省","衡阳市","demo@example.invalid","示例路 1 号"));companies.addMember(company.id(),client.getId(),"OWNER");}
}
