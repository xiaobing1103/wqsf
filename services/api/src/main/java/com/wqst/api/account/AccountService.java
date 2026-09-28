package com.wqst.api.account;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wqst.api.account.entity.UserAccountEntity;
import com.wqst.api.account.mapper.UserAccountMapper;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.CryptoService;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AccountService {
    private static final Set<String> TYPES=Set.of("ADMIN","CLIENT");
    private final UserAccountMapper users;private final PasswordEncoder passwords;private final CryptoService crypto;
    public AccountService(UserAccountMapper users,PasswordEncoder passwords,CryptoService crypto){this.users=users;this.passwords=passwords;this.crypto=crypto;}
    public List<AccountView> list(){return users.selectList(new LambdaQueryWrapper<UserAccountEntity>().isNull(UserAccountEntity::getDeletedAt).orderByDesc(UserAccountEntity::getCreatedAt)).stream().map(this::view).toList();}
    @Transactional public AccountView create(CreateAccount c){if(!TYPES.contains(c.accountType()))throw new BusinessException("ACCOUNT_TYPE_INVALID","账号类型无效",HttpStatus.BAD_REQUEST);if(StringUtils.hasText(c.username())&&users.findByUsername(c.username().trim())!=null)throw new BusinessException("ACCOUNT_USERNAME_EXISTS","用户名已存在",HttpStatus.CONFLICT);if("ADMIN".equals(c.accountType())&&(!StringUtils.hasText(c.password())||c.password().length()<12))throw new BusinessException("ACCOUNT_PASSWORD_WEAK","后台账号密码至少 12 位",HttpStatus.BAD_REQUEST);UserAccountEntity u=new UserAccountEntity();u.setAccountType(c.accountType());u.setUsername(trim(c.username()));u.setDisplayName(c.displayName().trim());u.setPasswordHash("ADMIN".equals(c.accountType())?passwords.encode(c.password()):null);u.setPhoneCipher(crypto.encrypt(c.phone()));u.setPhoneHash(crypto.hash(c.phone()));u.setPhoneMask(crypto.maskPhone(c.phone()));u.setStatus("ENABLED");users.insert(u);for(String role:c.roles()){if(users.roleExists(role)==0)throw new BusinessException("ROLE_NOT_FOUND","角色不存在: "+role,HttpStatus.BAD_REQUEST);users.assignRole(u.getId(),role);}return view(u);}
    @Transactional public AccountView roles(long userId,List<String> roles){UserAccountEntity u=users.selectById(userId);if(u==null)throw new BusinessException("ACCOUNT_NOT_FOUND","账号不存在",HttpStatus.NOT_FOUND);for(String role:roles)if(users.roleExists(role)==0)throw new BusinessException("ROLE_NOT_FOUND","角色不存在: "+role,HttpStatus.BAD_REQUEST);users.clearRoles(userId);roles.stream().distinct().forEach(r->users.assignRole(userId,r));return view(u);}
    private AccountView view(UserAccountEntity u){return new AccountView(u.getId(),u.getAccountType(),u.getUsername(),u.getDisplayName(),u.getPhoneMask(),u.getStatus(),users.findRoles(u.getId()),users.findPermissions(u.getId()),u.getLastLoginAt(),u.getCreatedAt());}
    private String trim(String v){return StringUtils.hasText(v)?v.trim():null;}
    public record CreateAccount(String accountType,String username,String password,String displayName,String phone,List<String> roles){}
    public record AccountView(Long id,String accountType,String username,String displayName,String phoneMask,String status,List<String> roles,List<String> permissions,java.time.LocalDateTime lastLoginAt,java.time.LocalDateTime createdAt){}
}
