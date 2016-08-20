//
// Copyright 2011-2012 Jeff Bush
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import java.io.InputStream;
import java.io.IOException;

//
// Classes override this interface to create an object that can load traces
// from a file.
//
interface TraceLoader
{
    /// @todo Some kind of file detection APIs (register by extension, sniff, etc)

    class TraceLoaderException extends Exception
    {
        TraceLoaderException(String description)
        {
            super(description);
        }
    }

    public void load(InputStream is, TraceBuilder builder) throws TraceLoaderException, IOException;
}

