package com.ecommerce.user_service.service;
import com.ecommerce.user_service.entity.User;import com.ecommerce.user_service.repository.UserRepository;import com.ecommerce.user_service.repository.UserAddressRepository;import jakarta.persistence.EntityNotFoundException;import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;
@Service @Transactional(readOnly=true) public class UserManagementService {

 private static final Set<String> ROLES=Set.of("ROLE_USER","ROLE_ADMIN","ROLE_DRIVER","ROLE_RESTAURANT_OWNER");
 private final UserRepository users;private final UserAddressRepository addresses;
 private final PasswordEncoder encoder;
 public UserManagementService(UserRepository r,UserAddressRepository a,PasswordEncoder e){users=r;addresses=a;encoder=e;}
 public Map<String,Object> get(long id){return view(find(id));}public List<Map<String,Object>> list(){
 return users.findAll().stream().map(this::view).toList();
}
 @Transactional public Map<String,Object> create(String name,String email,String password,String role){String normalized=email.trim().toLowerCase(Locale.ROOT);if(users.existsByEmail(normalized))throw new ResponseStatusException(HttpStatus.CONFLICT,"Email is already registered");User u=new User();u.setName(name.trim());u.setEmail(normalized);u.setPassword(encoder.encode(password));u.setRole(normalizeRole(role));return view(users.save(u));}
 @Transactional public Map<String,Object> updateProfile(long id,String name){User u=find(id);u.setName(name.trim());return view(users.save(u));}
 @Transactional public void changePassword(long id,String currentPassword,String newPassword){User u=find(id);if(!encoder.matches(currentPassword,u.getPassword()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password is incorrect");u.setPassword(encoder.encode(newPassword));users.save(u);}
 @Transactional public void resetPassword(long id,String newPassword){User u=find(id);u.setPassword(encoder.encode(newPassword));users.save(u);}
 @Transactional public Map<String,Object> update(long id,String name,String email,String role){User u=find(id);String normalized=email.trim().toLowerCase(Locale.ROOT);users.findByEmail(normalized).filter(existing->!existing.getId().equals(id)).ifPresent(existing->{throw new ResponseStatusException(HttpStatus.CONFLICT,"Email is already registered");});u.setName(name.trim());u.setEmail(normalized);u.setRole(normalizeRole(role));return view(users.save(u));}
 @Transactional public void delete(long id){users.delete(find(id));addresses.deleteByUserId(id);}
 private User find(long id){return users.findById(id).orElseThrow(()->new EntityNotFoundException("User not found"));}
 private String normalizeRole(String role){String value=role==null||role.isBlank()?"ROLE_USER":role.trim().toUpperCase(Locale.ROOT);if(!ROLES.contains(value))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid user role");return value;}
 private Map<String,Object> view(User u){return Map.of("id",u.getId(),"name",u.getName(),"email",u.getEmail(),"role",u.getRole());}
}
