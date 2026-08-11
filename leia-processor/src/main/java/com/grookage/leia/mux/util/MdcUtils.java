/*
 * Copyright (c) 2024-2025. Koushik R <rkoushik.14@gmail.com>.
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

package com.grookage.leia.mux.util;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

import java.util.Map;

@UtilityClass
public class MdcUtils {
	public static Runnable decorateWithMdc(Runnable task, Map<String, String> mdcContext) {
		return () -> {
			if (mdcContext != null) {
				MDC.setContextMap(mdcContext);
			}
			try {
				task.run();
			} finally {
				MDC.clear();
			}
		};
	}
}
