package com.diogorolins.springprj1.services;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.diogorolins.springprj1.domain.Address;
import com.diogorolins.springprj1.domain.Client;
import com.diogorolins.springprj1.domain.dto.ClientNewDTO;
import com.diogorolins.springprj1.domain.dto.ClientUpdateDTO;
import com.diogorolins.springprj1.domain.enums.ClientType;
import com.diogorolins.springprj1.domain.enums.Roles;
import com.diogorolins.springprj1.exceptions.AuthorizationException;
import com.diogorolins.springprj1.exceptions.DatabaseException;
import com.diogorolins.springprj1.exceptions.ObjectNotFoundException;
import com.diogorolins.springprj1.repositories.AddressRepository;
import com.diogorolins.springprj1.repositories.ClientRepository;
import com.diogorolins.springprj1.security.UserSS;

@Service
public class ClientService {
	
	@Autowired
	private ClientRepository repository;
	
	@Autowired
	private AddressRepository addressRepository;
	
	@Autowired
	private BCryptPasswordEncoder pe;
	
	@Autowired
	private S3Service s3Service;
	
	@Autowired
	private ImageService imageService;
	
	@Value("${img.prefix.client.profile}")
	private String prefix;
	
	@Value("${img.profile.size}")
	private Integer imageSize;
	
	public List<Client> findAll() {
		return repository.findAll();
	}
	
	public Client findById(Integer id) {
		UserSS userSS = UserService.authenticated();
		if(userSS == null || !userSS.hasRole(Roles.ADMIN) && !id.equals(userSS.getId())) {
			throw new AuthorizationException("Access denied.");
		}
		
		Optional<Client> obj = repository.findById(id);
		return obj.orElseThrow(() -> new ObjectNotFoundException("Resource not found: " + Client.class.getSimpleName() + " id " + id));
	}
	
	public Client insert(Client obj) {
		obj.setId(null);
		obj = repository.save(obj);
		addressRepository.saveAll(obj.getAddresses());
		return repository.save(obj);
	}
	
	public Client update(Client obj) {
		Client newObj = findById(obj.getId());
		obj = updateData(newObj, obj); 
		obj.getAddresses().stream().forEach(e -> System.out.println(e.getStreet()));
		repository.save(obj);
		return obj;
	}
	
	public void delete(Integer id) {
		findById(id);
		try {
			repository.deleteById(id);
		} catch(DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity error: " + Client.class.getSimpleName() + " ID: " + id );
		}
	}
	
	public Client findByEmail(String email) {
		UserSS userSS = UserService.authenticated();
		if(userSS == null || !userSS.hasRole(Roles.ADMIN) && !email.equals(userSS.getUsername())) {
			throw new AuthorizationException("Access denied.");
		}
		Client client = repository.findByEmail(email);
		if(client == null) {
			throw new ObjectNotFoundException("Email não encontrado.");
		}
		return client;
		
	}
	
	public Page<Client> findPage(Integer page, Integer linesPerPage, String orderBy, String direction ){
		PageRequest pageRequest = PageRequest.of(page, linesPerPage, Direction.valueOf(direction), orderBy);
		return repository.findAll(pageRequest);
	}
	
	public Client fromDto(@Valid ClientUpdateDTO dto) {
		Client cli =  new Client(null, dto.getName(), dto.getEmail(), dto.getCpfCnpj(), ClientType.toEnum(dto.getClientType()), dto.getPassword());
		List<Address> adrs = dto.getAddresses();
		for (Address address : adrs) {
			address.setClient(cli);
		}
		cli.getAddresses().addAll(adrs);
		cli.getPhones().addAll(dto.getPhones());
		return cli;
	}
	
	public Client fromDto(ClientNewDTO dto) {
		Client cli =  new Client(null, dto.getName(), dto.getEmail(), dto.getCpfCnpj(), ClientType.toEnum(dto.getClientType()), pe.encode(dto.getPassword()));
		List<Address> adrs = dto.getAddresses();
		for (Address address : adrs) {
			address.setClient(cli);
		}
		cli.getAddresses().addAll(adrs);
		cli.getPhones().addAll(dto.getPhones());
		return cli;
	}
	
	private Client updateData(Client newObj, Client obj) {
		newObj.setName(obj.getName());
		newObj.setCpfCnpj(obj.getCpfCnpj());
		newObj.setClientType(obj.getClientType());
		clearAddressList(obj.getId());
		newObj.getAddresses().clear();
		newObj.getAddresses().addAll(obj.getAddresses());
		
		
		newObj.getPhones().clear();
		newObj.getPhones().addAll(obj.getPhones());	
		
		return newObj;
	}
	
	private void clearAddressList(Integer id) {
		addressRepository.deletebyClient(id);
		
	}

	public URI uploadProfilePicture(MultipartFile mf) {
		UserSS userSS = UserService.authenticated();
		if(userSS == null) {
			throw new AuthorizationException("Access denied.");
		}
		BufferedImage jpgImage =imageService.getJpgImageFromFile(mf);
		jpgImage = imageService.cropSquare(jpgImage);
		jpgImage = imageService.resize(jpgImage, imageSize);
		
		String fileName = prefix + userSS.getId() + ".jpg";
		
		return s3Service.upUploadFile(imageService.getInputStream(jpgImage, "jpg"), fileName, "image");
	}

	public void changePassword(Integer id, String password) {
		Client newObj = findById(id);
		newObj.setPassword(pe.encode(password));	
		repository.save(newObj);
	}
		
}
