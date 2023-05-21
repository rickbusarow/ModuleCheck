#!/bin/bash

#
# Copyright (C) 2021-2023 Rick Busarow
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Ensure that `ncc` is installed
if ! type "ncc" > /dev/null; then
  npm i -g @vercel/ncc
fi

# Clean up any existing output directory
rm -rf dist

npm install

ncc build src/index.js --license licenses.txt

# Remove unnecessary files
rm -rf dist/node_modules

echo "Package created successfully in the 'dist' directory."
