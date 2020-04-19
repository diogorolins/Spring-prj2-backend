package com.diogorolins.springprj1.resources;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.diogorolins.springprj1.domain.Category;
import com.diogorolins.springprj1.services.CategoryService;

@RestController
@RequestMapping(value = "/categories")
public class CategoryResource {
	
	@Autowired
	private CategoryService service;

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public ResponseEntity<Category> findById(@PathVariable Integer id) {
		return ResponseEntity.ok().body(service.findById(id));
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<Category>> findAll(){
		return ResponseEntity.ok().body(service.findAll());
	}
	
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Category> insert(@RequestBody Category obj){
		URI uri = ServletUriComponentsBuilder
				.fromCurrentRequest().path("/{id}")
				.buildAndExpand(obj.getId()).toUri();
		return ResponseEntity.created(uri).body(service.insert(obj));
	}
	
	
}
