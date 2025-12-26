import glob

import pytest
import os
import requests
import json

cwd = os.getcwd()

URL = "http://localhost:8081/api/anonymization/flatjson"

# iterate over all testcases
@pytest.mark.parametrize('input', glob.glob(cwd + '/02_input_flat_json/*.json'))
def test(input):

    # Read the json body
    with open(input) as f:
        content = json.load(f)

    # Send the PUT request
    response = requests.put(URL, json=content)
    response_json = response.json()

    # Validate the input with the desired response
    output_path = input.replace('input', 'output')
    with open(output_path) as f:
        desired_output = json.load(f)
    assert response_json == desired_output

