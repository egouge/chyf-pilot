/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package net.refractions.chyf.rest.exceptions;

import org.springframework.validation.BindingResult;

public class InvalidParameterException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	private ErrorMessage message;
	
	public InvalidParameterException(BindingResult bindingResult) {
		message = new ErrorMessage(bindingResult);
	}
	
	public InvalidParameterException(ErrorMessage message) {
		this.message = message;
	}
	
	public ErrorMessage getErrorMessage() {
		return message;
	}
}
